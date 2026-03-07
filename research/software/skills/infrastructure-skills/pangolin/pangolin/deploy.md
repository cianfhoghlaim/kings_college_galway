---
name: Pangolin Deployment Helper
description: Deploy Pangolin applications to various platforms (Cloudflare, Netlify, Docker).
category: Deployment
tags: [pangolin, deployment, cloudflare, netlify, docker]
---

# Pangolin Deployment Assistant

I'll help you deploy your Pangolin application. First, let me gather information about your deployment target and environment.

## Deployment Checklist

Before we begin, ensure you have:
- [ ] All environment variables configured
- [ ] Tests passing (`npm run test`)
- [ ] Type checks passing (`npm run typecheck`)
- [ ] Linting passing (`npm run lint`)
- [ ] Production build succeeds (`npm run build`)

## Deployment Options

### Option 1: Cloudflare Workers (Recommended for Edge)
**Best for:** Global edge deployment, serverless compute, D1 database, R2 storage

**Steps:**
1. Install Wrangler CLI: `npm install -g wrangler`
2. Authenticate: `wrangler login`
3. Create D1 database: `wrangler d1 create pangolin-db`
4. Update `wrangler.toml` with database ID
5. Run migrations: `wrangler d1 migrations apply pangolin-db`
6. Deploy: `npm run deploy`

**Environment Variables (via wrangler.toml or secrets):**
```toml
[vars]
BETTER_AUTH_URL = "https://your-app.workers.dev"

# Set secrets via CLI:
# wrangler secret put BETTER_AUTH_SECRET
# wrangler secret put ANTHROPIC_API_KEY
# wrangler secret put GOOGLE_CLIENT_SECRET
```

### Option 2: Netlify Edge (Recommended for Full-Stack)
**Best for:** Fast deployment, edge functions, automatic deployments

**Steps:**
1. Install Netlify CLI: `npm install -g netlify-cli`
2. Login: `netlify login`
3. Initialize: `netlify init`
4. Set environment variables in Netlify dashboard
5. Deploy: `netlify deploy --prod`

**Environment Variables (via Netlify dashboard or CLI):**
```bash
netlify env:set BETTER_AUTH_SECRET "your-secret-32-chars-min"
netlify env:set BETTER_AUTH_URL "https://your-app.netlify.app"
netlify env:set ANTHROPIC_API_KEY "sk-ant-..."
netlify env:set CONVEX_DEPLOYMENT "your-convex-deployment"
```

### Option 3: Docker (Self-Hosted)
**Best for:** Complete control, private networks, Pangolin VPN integration

**Steps:**
1. Build image: `docker build -t pangolin-app .`
2. Create `.env` file with production values
3. Run container: `docker run -p 3000:3000 --env-file .env pangolin-app`
4. Or use Docker Compose: `docker compose up -d`

**Docker Compose Example:**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "3000:3000"
    environment:
      - BETTER_AUTH_SECRET=${BETTER_AUTH_SECRET}
      - BETTER_AUTH_URL=${BETTER_AUTH_URL}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
    restart: unless-stopped
```

### Option 4: Pangolin Network Proxy
**Best for:** Exposing services via HTTPS with VPN access

**Steps:**
1. Deploy app using any method above
2. Ensure app runs on internal network
3. Configure Pangolin proxy:
   ```bash
   cd infrastructure/compose/pangolin
   docker compose up -d
   ```
4. Register service in Pangolin dashboard
5. Access via:
   - Public: https://your-domain.com (Traefik HTTPS)
   - Private: VPN tunnel (WireGuard Olm client)

## Database Setup

### Convex (Real-time Backend)
1. Sign up at https://convex.dev
2. Create project: `npx convex dev`
3. Note deployment URL from output
4. Set `CONVEX_DEPLOYMENT` env var
5. Deploy functions: `npx convex deploy`

### Cloudflare D1 (SQLite at Edge)
```bash
# Create database
wrangler d1 create pangolin-db

# Apply schema
wrangler d1 execute pangolin-db --file=./schema.sql

# Run migrations
npm run db:migrate
```

### PostgreSQL (Traditional)
```bash
# Use Cloudflare Hyperdrive for connection pooling
wrangler hyperdrive create my-pg --connection-string="postgresql://user:pass@host:5432/db"

# Or use Supabase/Neon for managed PostgreSQL
```

## Storage Setup

### Cloudflare R2 (Object Storage)
```bash
# Create bucket
wrangler r2 bucket create pangolin-uploads

# Update wrangler.toml
[[r2_buckets]]
binding = "R2_BUCKET"
bucket_name = "pangolin-uploads"
```

### Cloudflare KV (Key-Value Cache)
```bash
# Create namespace
wrangler kv:namespace create "PANGOLIN_CACHE"

# Update wrangler.toml
[[kv_namespaces]]
binding = "KV_CACHE"
id = "your-namespace-id"
```

## OAuth Provider Setup

### Google OAuth
1. Go to https://console.cloud.google.com
2. Create project and enable Google+ API
3. Create OAuth 2.0 credentials
4. Add authorized redirect URI: `https://your-app.com/api/auth/callback/google`
5. Set env vars:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`

### GitHub OAuth
1. Go to https://github.com/settings/developers
2. Register new OAuth app
3. Add callback URL: `https://your-app.com/api/auth/callback/github`
4. Set env vars:
   - `GITHUB_CLIENT_ID`
   - `GITHUB_CLIENT_SECRET`

## Post-Deployment Verification

Run these checks after deployment:

```bash
# 1. Check health endpoint
curl https://your-app.com/api/health

# 2. Verify authentication works
curl -X POST https://your-app.com/api/auth/sign-in \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test"}'

# 3. Test API endpoints
curl https://your-app.com/api/rpc/chat.list \
  -H "Authorization: Bearer your-token"

# 4. Check Convex connection
# (Visit your app and open browser console, should see Convex connected)

# 5. Verify SSL certificate
openssl s_client -connect your-app.com:443 -servername your-app.com
```

## Monitoring & Observability

### Sentry (Error Tracking)
```bash
# Install Sentry
npm install @sentry/node @sentry/react

# Set environment variable
export SENTRY_DSN="your-sentry-dsn"

# Initialize in app
```

### Cloudflare Analytics
- Access via Cloudflare dashboard
- View requests, errors, performance metrics
- Set up alerts for high error rates

### Convex Dashboard
- View real-time function logs
- Monitor database queries
- Track API usage

## Rollback Procedure

If deployment fails or has issues:

### Netlify
```bash
# List deployments
netlify sites:list

# Rollback to previous
netlify rollback
```

### Cloudflare Workers
```bash
# List deployments
wrangler deployments list

# Rollback to specific version
wrangler rollback --version-id <version-id>
```

### Docker
```bash
# Stop current container
docker stop pangolin-app

# Start previous version
docker run -d --name pangolin-app pangolin-app:previous

# Or use Docker Compose
docker compose down
git checkout previous-commit
docker compose up -d
```

## Troubleshooting

### Build Fails
- Check Node.js version (requires 20+)
- Clear cache: `rm -rf node_modules/.vite && npm install`
- Verify all dependencies installed: `npm install`

### Environment Variables Not Set
- Double-check spelling and values
- Restart deployment after setting vars
- Check platform-specific syntax (Netlify vs Cloudflare)

### Database Connection Fails
- Verify connection string format
- Check database is running and accessible
- Ensure migrations are applied
- Test connection locally first

### SSL Certificate Issues
- Wait 1-2 minutes for Let's Encrypt provisioning
- Verify domain DNS points to deployment
- Check Cloudflare SSL/TLS mode (Full or Flexible)

## Security Hardening

Before going to production:

1. **Secrets Management**
   - Use platform secret stores (not env vars in code)
   - Rotate secrets regularly
   - Never commit `.env` files

2. **Rate Limiting**
   - Enable Cloudflare rate limiting
   - Implement per-user API quotas
   - Add CAPTCHA for sensitive operations

3. **Content Security Policy**
   - Set CSP headers to prevent XSS
   - Configure in Netlify `_headers` or Workers

4. **CORS Configuration**
   - Whitelist only trusted origins
   - Use credentials: true only when needed

## Next Steps

Tell me:
1. Which deployment platform do you prefer? (Cloudflare, Netlify, Docker)
2. Do you have existing infrastructure? (Pangolin VPN, Komodo)
3. What's your database choice? (Convex, D1, PostgreSQL)
4. Do you need OAuth configured? (Google, GitHub)

I'll provide step-by-step guidance tailored to your setup!
