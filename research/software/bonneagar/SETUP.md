# Automation Setup Guide

Complete checklist for running Ansible playbooks via Docker Compose with 1Password Connect for secrets management.

## Prerequisites Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         YOUR LOCAL MACHINE                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Docker Compose                                                      │   │
│  │  ├── 1Password Connect (op-connect-api + op-connect-sync)           │   │
│  │  │   └── Requires: 1password-credentials.json + access token        │   │
│  │  └── Ansible Execution Environment                                   │   │
│  │      └── Requires: SSH key to reach remote servers                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      │ SSH (port 22)                        │
│                                      ▼                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
            ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
            │  Server 1   │    │  Server 2   │    │  Server N   │
            │  (target)   │    │  (target)   │    │  (target)   │
            └─────────────┘    └─────────────┘    └─────────────┘
            Requirements:       Requirements:       Requirements:
            - SSH access        - SSH access        - SSH access
            - sudo rights       - sudo rights       - sudo rights
            - Python 3          - Python 3          - Python 3
```

---

## Step 1: 1Password Connect Setup

### 1.1 Create a Connect Server in 1Password

1. Log in to your 1Password account at https://my.1password.com
2. Go to **Integrations** → **Directory** → **1Password Connect Server**
3. Click **Add Connect Server**
4. Name it (e.g., "Bunchloch Automation")
5. Choose which vaults to grant access (select vaults containing your secrets)
6. **Download the credentials file** → Save as `1password-credentials.json`

### 1.2 Create an Access Token

1. Still in 1Password Integrations, go to **Access Tokens**
2. Click **Create Token**
3. Name it (e.g., "Ansible Automation")
4. Select which vaults this token can access
5. **Copy the token** - you'll only see it once!
6. Save it securely (we'll use it later)

### 1.3 Deploy the Credentials File

```bash
# Navigate to the 1Password compose directory
cd /Users/cliste/dev/bonneagar/hackathon/infrastructure/compose/bunchloch/onepassword

# Copy your downloaded credentials file here
cp ~/Downloads/1password-credentials.json ./1password-credentials.json

# Secure the file
chmod 600 1password-credentials.json
```

### 1.4 Start 1Password Connect

```bash
cd /Users/cliste/dev/bonneagar/hackathon/infrastructure/compose/bunchloch/onepassword

# Start Connect services
docker compose up -d

# Verify it's running
curl http://localhost:8080/heartbeat
# Should return: {"version":"..."}
```

### 1.5 Save the Access Token

Create a token file for the automation:

```bash
# Create directory for the token
sudo mkdir -p /etc/connect

# Save your access token (replace with your actual token)
echo "your-access-token-here" | sudo tee /etc/connect/token > /dev/null

# Secure it
sudo chmod 600 /etc/connect/token
```

---

## Step 2: SSH Key Setup

### 2.1 Generate or Locate SSH Key

```bash
# Option A: Generate a new key for Ansible
ssh-keygen -t ed25519 -f ~/.ssh/ansible -C "ansible-automation"

# Option B: Use existing key
ls -la ~/.ssh/id_ed25519
```

### 2.2 Copy Public Key to Remote Servers

For each server you want to manage:

```bash
# Replace with your actual server IP and user
ssh-copy-id -i ~/.ssh/ansible.pub your_user@10.1.10.4
ssh-copy-id -i ~/.ssh/ansible.pub your_user@10.1.10.5
ssh-copy-id -i ~/.ssh/ansible.pub your_user@10.1.10.6
```

### 2.3 Test SSH Connection

```bash
# Test without password prompt
ssh -i ~/.ssh/ansible your_user@10.1.10.4 "echo 'SSH works!'"
```

---

## Step 3: Remote Server Requirements

On each target server, ensure:

### 3.1 SSH Access

```bash
# SSH server must be running
sudo systemctl status sshd

# Port 22 (or your SSH port) must be open
sudo ufw allow 22/tcp   # Ubuntu/Debian
# or
sudo firewall-cmd --add-service=ssh --permanent && sudo firewall-cmd --reload  # RHEL/CentOS
```

### 3.2 Sudo Access

The Ansible user must have passwordless sudo OR you must provide the password:

```bash
# Option A: Passwordless sudo (add to /etc/sudoers.d/)
echo "your_user ALL=(ALL) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/ansible

# Option B: Password-based sudo (you'll encrypt the password with Vault)
# Just ensure the user is in the sudo/wheel group
sudo usermod -aG sudo your_user  # Debian/Ubuntu
sudo usermod -aG wheel your_user  # RHEL/CentOS
```

### 3.3 Python 3

```bash
# Ansible requires Python on target hosts
python3 --version

# Install if missing
sudo apt install python3  # Debian/Ubuntu
sudo dnf install python3  # RHEL/CentOS
```

### 3.4 Docker (for Periphery to manage containers)

```bash
# Docker must be installed for Periphery to work
docker --version

# If not installed, follow Docker's official guide:
# https://docs.docker.com/engine/install/
```

---

## Step 4: Update Compose Configuration

### 4.1 Update automation/compose.yaml

```yaml
---
services:
  ansible:
    image: ghcr.io/bpbradley/ansible/komodo-ee:v2.0
    extra_hosts:
      - host.docker.internal:host-gateway
    volumes:
      # Mount ansible files
      - ./ansible:/ansible
      # Mount your SSH private key (update path!)
      - ~/.ssh/ansible:/root/.ssh/id_ed25519:ro
      # Mount 1Password token for locket verification
      - /etc/connect/token:/etc/connect/token:ro
    environment:
      ANSIBLE_HOST_KEY_CHECKING: "false"
      # Vault password (set when running)
      ANSIBLE_VAULT_PASSWORD_FILE: /tmp/.vaultpass
      # 1Password Connect (for pre-flight checks)
      OP_CONNECT_HOST: "http://host.docker.internal:8080"
    command: "sleep 3600"

networks:
  default:
    name: pangolin
    external: true
```

---

## Step 5: Create Ansible Vault Password

### 5.1 Generate and Encrypt Credentials

```bash
cd /Users/cliste/dev/bonneagar/hackathon/infrastructure/compose/bunchloch/automation

# Start the ansible container
docker compose up -d

# Exec into it
docker compose exec ansible bash

# Inside container: Generate vault password
head /dev/urandom | tr -dc _A-Z-a-z-0-9 | head -c64 > /tmp/.vaultpass
export ANSIBLE_VAULT_PASSWORD_FILE=/tmp/.vaultpass

# Encrypt your sudo password
ansible-vault encrypt_string "your_sudo_password" --name "ansible_become_pass"
# Copy the output

# Encrypt your 1Password Connect token
ansible-vault encrypt_string "your-1password-token" --name "locket_op_connect_token"
# Copy the output

# Show vault password (save this securely!)
cat /tmp/.vaultpass
```

### 5.2 Update Inventory with Encrypted Values

Edit `ansible/inventory/komodo.yml` and replace the placeholder vault strings with your encrypted values.

### 5.3 Save Vault Password for Komodo

If using Komodo Actions:
1. Go to **Komodo → Settings → Variables**
2. Create variable `VAULT_PASS` with your vault password
3. Mark it as **Secret**

---

## Step 6: Update Inventory with Real Values

Edit `/Users/cliste/dev/bonneagar/hackathon/infrastructure/compose/bunchloch/automation/ansible/inventory/komodo.yml`:

```yaml
all:
  hosts:
    # Replace with your actual server names and IPs
    server1:
      ansible_host: 10.1.10.4
    server2:
      ansible_host: 10.1.10.5

  vars:
    # Replace with your actual SSH user
    ansible_user: your_actual_username

    # Your encrypted sudo password (from Step 5)
    ansible_become_pass: !vault |
            $ANSIBLE_VAULT;1.1;AES256
            ... your encrypted string ...

    # SSH key path inside container
    ansible_ssh_private_key_file: /root/.ssh/id_ed25519

    # Your Komodo Core URL
    periphery_core_address: "wss://komodo.yourdomain.com"

    # 1Password Connect (accessible from target servers)
    locket_op_connect_host: "http://your-connect-host:8080"

    # Your encrypted 1Password token (from Step 5)
    locket_op_connect_token: !vault |
            $ANSIBLE_VAULT;1.1;AES256
            ... your encrypted string ...
```

---

## Step 7: Test the Setup

### 7.1 Test Ansible Connectivity

```bash
cd /Users/cliste/dev/bonneagar/hackathon/infrastructure/compose/bunchloch/automation

# Start container
docker compose up -d

# Test ping all hosts
docker compose exec ansible ansible all -i /ansible/inventory/komodo.yml -m ping

# Expected output:
# server1 | SUCCESS => { "ping": "pong" }
# server2 | SUCCESS => { "ping": "pong" }
```

### 7.2 Dry Run the Playbook

```bash
# Dry run (check mode)
docker compose exec ansible ansible-playbook \
  -i /ansible/inventory/komodo.yml \
  /ansible/playbooks/periphery.yml \
  --check --diff
```

### 7.3 Run for Real

```bash
# Full deployment
docker compose exec ansible ansible-playbook \
  -i /ansible/inventory/komodo.yml \
  /ansible/playbooks/periphery.yml
```

---

## Quick Reference: What You Need

| Item | Where to Get It | Where to Put It |
|------|-----------------|-----------------|
| **1password-credentials.json** | 1Password.com → Integrations → Connect Server | `onepassword/1password-credentials.json` |
| **1Password Access Token** | 1Password.com → Integrations → Access Tokens | `/etc/connect/token` + vault-encrypted in inventory |
| **SSH Private Key** | `~/.ssh/ansible` or existing key | Mount in `compose.yaml` |
| **SSH Public Key** | `~/.ssh/ansible.pub` | Copy to each remote server |
| **Sudo Password** | Your server's sudo password | Vault-encrypted in inventory |
| **Vault Password** | Generate with `head /dev/urandom...` | Komodo secret variable `VAULT_PASS` |
| **Server IPs** | Your infrastructure | `inventory/komodo.yml` |
| **SSH User** | User with sudo on remote servers | `inventory/komodo.yml` |

---

## Network Requirements

### From Ansible Container (your machine)

| Destination | Port | Protocol | Purpose |
|-------------|------|----------|---------|
| Target servers | 22 | TCP | SSH |
| 1Password Connect | 8080 | TCP | Secrets API |

### From Target Servers

| Destination | Port | Protocol | Purpose |
|-------------|------|----------|---------|
| Komodo Core | 9120 | TCP (WSS) | Periphery → Core connection |
| 1Password Connect | 8080 | TCP | Locket secrets (optional*) |

*Only needed if target servers will use locket directly. For initial deployment, only the Ansible host needs Connect access.

---

## Troubleshooting

### SSH Connection Failed

```bash
# Test SSH manually
ssh -vvv -i ~/.ssh/ansible user@server_ip

# Common fixes:
# - Check firewall: sudo ufw status
# - Check SSH service: sudo systemctl status sshd
# - Check key permissions: chmod 600 ~/.ssh/ansible
```

### Vault Decryption Failed

```bash
# Verify vault password is set
echo $ANSIBLE_VAULT_PASSWORD_FILE
cat $ANSIBLE_VAULT_PASSWORD_FILE

# Test decryption
ansible-vault decrypt_string --vault-password-file /tmp/.vaultpass
# Paste encrypted string, Ctrl+D
```

### 1Password Connect Unreachable

```bash
# Check Connect is running
docker ps | grep 1password

# Check heartbeat
curl http://localhost:8080/heartbeat

# Check logs
docker logs op-connect-api
```

### Permission Denied on Remote Server

```bash
# Ensure user has sudo
ssh user@server "sudo -n whoami"

# If password required, verify encrypted password in inventory
# Re-encrypt if needed:
ansible-vault encrypt_string "correct_password" --name "ansible_become_pass"
```
