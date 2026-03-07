---
title: "How to Run and Deploy LLMs on your iOS or Android Phone | Unsloth Documentation"
source: "https://docs.unsloth.ai/new/deploy-llms-phone"
author:
published: 2025-12-18
created: 2025-12-20
description: "Tutorial for fine-tuning your own LLM and deploying it on your Android or iPhone with ExecuTorch."
tags:
  - "clippings"
---
We’re excited to show how you can train LLMs then **deploy them locally** to **Android phones** and **iPhones**. We collabed with [ExecuTorch](https://github.com/pytorch/executorch/) from PyTorch & Meta to create a streamlined workflow using quantization-aware training ([QAT](https://docs.unsloth.ai/basics/quantization-aware-training-qat)) then deploy them directly to edge devices. With [Unsloth](https://github.com/unslothai/unsloth), TorchAO and ExecuTorch, we show how you can:

- Use the same tech (ExecuTorch) Meta has to power billions on Instagram, WhatsApp
- Deploy Qwen3-0.6B locally to **Pixel 8** and **iPhone 15 Pro at ~40 tokens/s**
- Apply QAT via TorchAO to recover 70% of accuracy
- Use our [free Colab notebook](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen3_\(0_6B\)-Phone_Deployment.ipynb) to fine-tune Qwen3 0.6B and export it for phone deployment

[iOS Tutorial](https://docs.unsloth.ai/new/deploy-llms-phone#ios-deployment) [Android Tutorial](https://docs.unsloth.ai/new/deploy-llms-phone#android-deployment)

**Qwen3-4B** deployed on a iPhone 15 Pro

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252F7tFjmj9c3p6o4eN3oHQq%252Funknown.png%3Falt%3Dmedia%26token%3D009699b3-e48f-4a94-bcd0-26cf6dedb8eb&width=768&dpr=4&quality=100&sign=ff8f6bf4&sv=2)

**Qwen3-0.6B** running at ~40 tokens/s

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FWI9nU1RQVrPbVXrIihfA%252Fimage.png%3Falt%3Dmedia%26token%3D5d58eb94-aeb3-42c3-a891-561ceb4e22db&width=768&dpr=4&quality=100&sign=59395c0d&sv=2)

We support Qwen3, Gemma3, Llama3, Qwen2.5, Phi4 and many other models for phone deployment! Follow the [**free Colab notebook**](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen3_\(0_6B\)-Phone_Deployment.ipynb) **for Qwen3-0.6B deployment:**

[Google Colab colab.research.google.com](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen3_\(0_6B\)-Phone_Deployment.ipynb)

First update Unsloth and install TorchAO and Executorch.

```
pip install --upgrade unsloth unsloth_zoo

pip install torchao==0.14.0 executorch pytorch_tokenizers
```

Then simply use `qat_scheme = "phone-deployment"` to signify we want to deploy it to a phone. Note we also set `full_finetuning = True` for full finetuning!

```
from unsloth import FastLanguageModel

import torch

model, tokenizer = FastLanguageModel.from_pretrained(

    model_name = "unsloth/Qwen3-0.6B",

    max_seq_length = 1024,

    full_finetuning = True,

    qat_scheme = "phone-deployment", # Flag for phone deployment

)
```

We’re using `qat_scheme = "phone-deployment"` we actually use `qat_scheme = "int8-int4"` under the hood to enable Unsloth/TorchAO QAT that *simulates* INT8 dynamic activation quantization with INT4 weight quantization for Linear layers during training (via fake quantization operations) while keeping computations in 16bits. After training, the model is converted to a real quantized version so the on-device model is smaller and typically **retains accuracy better than naïve PTQ**.

After finetuning as described in the [Colab notebook](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen3_\(0_6B\)-Phone_Deployment.ipynb), we then save it to a `.pte` file via Executorch:

```
# Convert the weight checkpoint state dict keys to one that ExecuTorch expects

python -m executorch.examples.models.qwen3.convert_weights "phone_model" pytorch_model_converted.bin

# Download model config from ExecuTorch repo

curl -L -o 0.6B_config.json https://raw.githubusercontent.com/pytorch/executorch/main/examples/models/qwen3/config/0_6b_config.json

# Export to ExecuTorch pte file

python -m executorch.examples.models.llama.export_llama \

    --model "qwen3_0_6b" \

    --checkpoint pytorch_model_converted.bin \

    --params 0.6B_config.json \

    --output_name qwen3_0.6B_model.pte \

    -kv --use_sdpa_with_kv_cache -X --xnnpack-extended-ops \

    --max_context_length 1024 --max_seq_length 128 --dtype fp32 \

    --metadata '{"get_bos_id":199999, "get_eos_ids":[200020,199999]}'
```

And now with your `qwen3_0.6B_model.pte` file which is around 472MB in size, we can deploy it! Pick your device and jump straight in:

- [iOS Deployment](https://docs.unsloth.ai/new/deploy-llms-phone#ios-deployment) – Xcode route, simulator or device
- [Android Deployment](https://docs.unsloth.ai/new/deploy-llms-phone#android-deployment) – command-line route, no Studio required

Tutorial to get your model running on iOS (tested on an iPhone 16 Pro but will work for other iPhones too). You will need a physical macOS based device which must be capable of running XCode 15.

**Install Xcode & Command Line Tools**

1. Install Xcode from the Mac App Store (must be version 15 or later)
2. Open Terminal and verify your installation: `xcode-select -p`
3. Install command line tools and accept the license:
	1. `xcode-select --install`
	2. `sudo xcodebuild -license accept`
4. Launch Xcode for the first time and install any additional components when prompted
5. If asked to select platforms, choose iOS 18 and download it for simulator access

Important: The first Xcode launch is crucial! Don't skip those extra component installations! Check [here](https://developer.apple.com/documentation/xcode/downloading-and-installing-additional-xcode-components) and [here](https://developer.apple.com/documentation/safari-developer-tools/adding-additional-simulators) for additional help.

**Verify Everything Works:** `xcode-select -p`

You should see a path printed. If not, repeat step 3.

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FJii1jArd6GQrdaCMHvyR%252Funknown.png%3Falt%3Dmedia%26token%3Dbd8b7a75-23e3-4474-b84b-ab9ad34cc401&width=300&dpr=4&quality=100&sign=3e18ea23&sv=2)

**For Physical devices only!**

**Create Your Apple ID**

Don't have an Apple ID? [Sign up here](https://support.apple.com/en-us/108647?device-type=iphone).

1. Open Xcode
2. Click the + button and select Apple ID

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FxG5ifHNeI6xKWqHw1pxL%252Funknown.png%3Falt%3Dmedia%26token%3D875fb5e4-e5f3-4c88-9af6-cb4e587975ca&width=768&dpr=4&quality=100&sign=1d976fcd&sv=2)

ExecuTorch requires the `increased-memory-limit capability`, which needs a paid developer account:

1. Visit [developer.apple.com](https://developer.apple.com/)
2. Enroll in the Apple Developer Program

**Grab the Example Code:**

```
# Download the LLM example app directly

curl -L https://github.com/meta-pytorch/executorch-examples/archive/main.tar.gz | \

  tar -xz --strip-components=2 executorch-examples-main/llm/apple
```

**Open in Xcode**

1. Open `apple/etLLM.xcodeproj` in Xcode
2. In the top toolbar, select `iPhone 16 Pro` Simulator as your target device
3. Hit Play (▶️) to build and run

🎉 Success! The app should now launch in the simulator. It won't work yet, we need to add your model.

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FA4n2u44u9sLlauCkhf1b%252Funknown.png%3Falt%3Dmedia%26token%3Dc93fef18-aab6-47cb-b301-d895466314f6&width=768&dpr=4&quality=100&sign=ff469bdb&sv=2)

**No Developer Account is needed.**

**Prepare Your Model Files**

1. Stop the simulator in Xcode (press the stop button)
2. Download these two files:
	1. `qwen3_0.6B_model.pte` (your exported model)
	2. tokenizer.json (the tokenizer)

**Create a Shared Folder on the Simulator**

1. Click the virtual Home button on the simulator
2. Open the Files App → Browse → On My iPhone
3. Tap the ellipsis (•••) button and create a new folder named `Qwen3test`

**Transfer Files Using the Terminal**

```
# Find the simulator's hidden folder

find ~/Library/Developer/CoreSimulator/Devices/ -type d -iname "*Qwen3test*"
```

When you see the folder run the following:

```
cp tokenizer.json /path/to/Qwen3test/tokenizer.json

cp qwen3_0.6B_model.pte /path/to/Qwen3test/qwen3_model.pte
```

**Load & Chat**

1. Return to the etLLM app in the simulator. Tap it to launch.
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252F55YWFJN49DCiHsy9EKOA%252Funknown.png%3Falt%3Dmedia%26token%3D4f8c8e90-df0b-4121-99eb-24437580724b&width=768&dpr=4&quality=100&sign=7cfc34aa&sv=2)

1. Load the model and tokenizer from the Qwen3test folder
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FpwUCX0nfarr6HSUd0pd3%252Funknown.png%3Falt%3Dmedia%26token%3D923b6ad3-d6e6-4e64-8223-947410c2218e&width=768&dpr=4&quality=100&sign=65601272&sv=2)

1. Start chatting with your fine-tuned model! 🎉
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FJrEzy1bvVeb4qLFxPFit%252Funknown.png%3Falt%3Dmedia%26token%3D36b7c70b-f014-4323-bdc5-cc5bf0fd12af&width=768&dpr=4&quality=100&sign=c00e5698&sv=2)

**Initial Device Setup**

1. Connect your iPhone to your Mac via USB
2. Unlock your iPhone and tap "Trust This Device"
3. In Xcode, go to Window → Devices and Simulators
4. Wait until your device appears on the left (it may show "Preparing" for a bit)

**Configure Xcode Signing**

1. Add your Apple Account: Xcode → Settings → Accounts → `+`
2. Select etLLM under TARGETS
3. Go to the Signing & Capabilities tab
4. Check "Automatically manage signing"
5. Select your Team from the dropdown

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FFm4a47e9Wuo7JiNbEeYl%252Funknown.png%3Falt%3Dmedia%26token%3D3f958363-6c0d-4608-8895-8376b0e1b1b1&width=768&dpr=4&quality=100&sign=ecb063f9&sv=2)

Change the Bundle Identifier to something unique (e.g., com.yourname.etLLM). This fixes 99% of provisioning profile errors

**Add the Required Capability**

1. Still in Signing & Capabilities, click + Capability
2. Search for "Increased Memory Limit" and add it

**Build & Run**

1. In the top toolbar, select your physical iPhone from the device selector
2. Hit Play (▶️) or press Cmd + R

**Trust the Developer Certificate**

Your first build will fail—this is normal!

1. Toggle On
2. Agree and accept notices
3. Restart device, return to Xcode and hit Play again

Developer Mode allows XCode to run and install apps on your iPhone

**Transfer Model Files to Your iPhone**

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FqAGQov6BgjlDSqA5GENN%252Funknown.png%3Falt%3Dmedia%26token%3D386b17df-703c-4e2c-9969-895577a98f0a&width=768&dpr=4&quality=100&sign=cf83faed&sv=2)
1. Once the app is running, open Finder on your Mac
2. Click the Files tab
3. Expand etLLM
4. Drag and drop your.pte and tokenizer.json files directly into this folder
5. Be patient! These files are large and may take a few minutes

**Load & Chat**

1. On your iPhone, switch back to the etLLM app
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FXY4EPFNcxaaBpjVroja3%252Funknown.jpeg%3Falt%3Dmedia%26token%3D7e8eca62-a5de-4705-9f0c-832b40579e78&width=768&dpr=4&quality=100&sign=7be7dba3&sv=2)
1. Load the model and tokenizer from the app interface
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FUzKWYRNR02vkVn5S3SQ5%252Funknown.jpeg%3Falt%3Dmedia%26token%3D84a85440-bf98-438d-a035-d8a11912a7a8&width=768&dpr=4&quality=100&sign=9b3f9639&sv=2)

1. Your fine-tuned Qwen3 is now running natively on your iPhone!
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FBX1nCLPbsnuRQchJXyAS%252Funknown.png%3Falt%3Dmedia%26token%3Dd276d4d6-2fc7-4cba-87f1-634aaea29884&width=768&dpr=4&quality=100&sign=57c97c56&sv=2)

This guide covers how to build and install the ExecuTorch Llama demo app on an Android device (tested using Pixel 8 but will also work on other Android phones too) using a Linux/Mac command line environment. This approach minimizes dependencies (no Android Studio required) and offloads the heavy build process to your computer.

### 🚀 Requirements

Ensure your development machine has the following installed:

- Java 17 (Java 21 is often the default but may cause build issues)
- Git
- Wget / Curl
- Android Command Line Tools
- [Guide to install](https://www.xda-developers.com/install-adb-windows-macos-linux/) and setup `adb` on your android and your computer

#### Verification

Check that your Java version matches 17.x:

```
# Output should look like: openjdk version "17.0.x"

java -version
```

If it does not match, install it via Ubuntu/Debian:

```
sudo apt install openjdk-17-jdk
```

Then set it as default or export `JAVA_HOME`:

```
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

export PATH=$JAVA_HOME/bin:$PATH
```

If you are on a different OS or distribution, you might want to follow [this guide](https://docs.oracle.com/en/java/javase/25/install/overview-jdk-installation.html) or just ask your favorite LLM to guide you through.

Set up a minimal Android SDK environment without the full Android Studio.

1\. Create the SDK directory:

```
mkdir -p ~/android-sdk/cmdline-tools

cd ~/android-sdk
```

1. Install Android Command Line Tools

```
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

unzip commandlinetools-linux-*.zip -d cmdline-tools

# Important: Reorganize to satisfy SDK structure

mv cmdline-tools/cmdline-tools cmdline-tools/latest
```

Add these to your `~/.bashrc` or `~/.zshrc`:

```
export ANDROID_HOME=$HOME/android-sdk

export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

export PATH=$ANDROID_HOME/platform-tools:$PATH
```

Reload them:

```
source ~/.zshrc  # or ~/.bashrc depending on your shell
```

ExecuTorch requires specific NDK versions.

```
# Accept licenses

yes | sdkmanager --licenses

# Install API 34 and NDK 25

sdkmanager "platforms;android-34" "platform-tools" "build-tools;34.0.0" "ndk;25.0.8775105"
```

Set the NDK variable:

```
export ANDROID_NDK=$ANDROID_HOME/ndk/25.0.8775105
```

We use the `executorch-examples` repository, which contains the updated Llama demo.

```
cd ~

git clone https://github.com/meta-pytorch/executorch-examples.git

cd executorch-examples
```

Note that the current code doesn't have these issues but we have faced them previously and might be helpful to you:

**Fix "SDK Location not found":**

Create a `local.properties` file to explicitly tell Gradle where the SDK is:

```
echo "sdk.dir=$HOME/android-sdk" > llm/android/LlamaDemo/local.properties
```

**Fix** `**cannot find symbol**` **error:**

The current code uses a deprecated method `getDetailedError()`. Patch it with this command:

```
sed -i 's/e.getDetailedError()/e.getMessage()/g' llm/android/LlamaDemo/app/src/main/java/com/example/executorchllamademo/MainActivity.java
```

This step compiles the app and native libraries.

1. Build with Gradle (explicitly set `JAVA_HOME` to 17 to avoid toolchain errors):
	Note: The first run will take a few minutes.
	```
	export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
	./gradlew :app:assembleDebug
	```
2. The final generated apk can be found at:
	```
	app/build/outputs/apk/debug/app-debug.apk
	```

You have two options to install the app.

If you have `adb` access to your phone:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If you are on a remote VM or don't have a cable:

1. Upload the app-debug.apk to a place where you can download from on the phone
2. Download it on your phone
3. Tap to Install (Enable "Install from unknown sources" if prompted).

The app needs the.pte model and tokenizer files.

1. Transfer Files: Move your model.pte and tokenizer.bin (or tokenizer.model) to your phone's storage (e.g., Downloads folder).
2. Open LlamaDemo App: Launch the app on your phone.
3. Select Model
4. Tap the Settings (gear icon) or the file picker.
5. Select your.pte file.
6. Select your tokenizer file.

Done! You can now chat with the LLM directly on your device.

### Troubleshooting

- Build Fails? Check java -version. It MUST be 17.
- Model not loading? Ensure you selected both the `.pte` AND the `tokenizer`.
- App crashing? Valid `.pte` files must be exported specifically for ExecuTorch (usually XNNPACK backend for CPU).

Currently, `executorchllama` app that we built only supports loading the model from a specific directory on Android that is unfortunately not accessible via regular file managers. But we can save the model files to the said directory using adb.

```
adb devices
```

1. If you have connected via wireless debugging, you’d see something like this:
	![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FX1uYoIhXRdboBK36FX9D%252Funknown.png%3Falt%3Dmedia%26token%3D32955e17-56b7-4e2c-a06d-a1558d51427b&width=768&dpr=4&quality=100&sign=f8c53a7a&sv=2)
	Or if you have connected via a wire/cable:
	![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FBu88g0y9ivw0UQYsUyJJ%252Funknown.png%3Falt%3Dmedia%26token%3D8eda0918-398f-486d-a1f2-6976f895a7c2&width=768&dpr=4&quality=100&sign=84093363&sv=2)
	If you haven’t given permissions to the computer to access your phone:
	![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FSFkcwJyvgTcjvsPzCoDc%252Funknown.png%3Falt%3Dmedia%26token%3Dcb4bbdb6-4b83-473c-8a96-bbf75d8ba49e&width=768&dpr=4&quality=100&sign=ccc4301&sv=2)

1. Then you need to check your phone for a pop up dialog that looks like (which you might want to allow)
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FfqqtrC2590Wd71uzzbA5%252Funknown.png%3Falt%3Dmedia%26token%3De9a15b34-d794-47d1-ac63-cc5809f3e650&width=768&dpr=4&quality=100&sign=9696b8af&sv=2)

Once done, it's time to create the folder where we need to place the `.pte` and `tokenizer.json` files.

Create the said directory on the phone’s path.

```
adb shell mkdir -p /data/local/tmp/llama

adb shell chmod 777 /data/local/tmp/llama
```

Verify that the directory is created properly.

```
adb shell ls -l /data/local/tmp/llama

total 0
```

Push the contents to the said directory. This might take a couple of minutes to more depending on your computer, the connection and the phone. Please be patient.

```
adb push <path_to_tokenizer.json on your computer> /data/local/tmp/llama

adb push <path_to_model.pte on your computer> /data/local/tmp/llama
```

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FwqtWYiRBiyAOhi3aecn9%252Fimage.png%3Falt%3Dmedia%26token%3Dab04a1d1-194d-420d-a980-3336f90e7e42&width=768&dpr=4&quality=100&sign=493d4305&sv=2)

1. Open the `executorchllamademo` app you installed in Step 5, then tap the gear icon in the top-right to open Settings.
2. Tap the arrow next to Model to open the picker and select a model. If you see a blank white dialog with no filename, your ADB model push likely failed - redo that step. Also note it may initially show “no model selected.”
3. After you select a model, the app should display the model filename.

![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FmwIP3Fg2xWNfq5h719rE%252Funknown.png%3Falt%3Dmedia%26token%3D3b560fc2-6820-4dd1-a8fa-1a76e5523672&width=768&dpr=4&quality=100&sign=b5904ec4&sv=2) ![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252F5ft9HycpKPtCYhWgTmMn%252Funknown.png%3Falt%3Dmedia%26token%3Ddc35909b-9541-4fb1-9c7a-7a4be242afd4&width=768&dpr=4&quality=100&sign=b31c94e4&sv=2)

1. Now repeat the same for tokenizer. Click on the arrow next to the tokenizer field and select the corresponding file.
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252Fhga4tR05b5D0IqLvB2PM%252Funknown.png%3Falt%3Dmedia%26token%3Dfb00738e-9429-4014-836d-3e35821279cd&width=768&dpr=4&quality=100&sign=4b9e4dea&sv=2)

1. You might need to select the model type depending on which model you're uploading. Qwen3 is selected here.
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FjAZd67Ruub3gfblDrwUs%252Funknown.png%3Falt%3Dmedia%26token%3Dcf0f6938-2e9c-4bf4-b0f2-c7512b5506ad&width=768&dpr=4&quality=100&sign=f9b20bc&sv=2)

1. Once you have selected both files, click on the "Load Model" button.
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FGaPBdnweeeRIWgWsK9Fg%252Funknown.png%3Falt%3Dmedia%26token%3D73ec7e74-d9f8-4080-a6b0-ef239fd640d9&width=768&dpr=4&quality=100&sign=e43603e1&sv=2)

1. It will take you back to the original screen with the chat window, and it might show "model loading". It might take a few seconds to finish loading depending on your phone's RAM and storage speeds.
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252F1XHwMpnWEB2JiwNAR6hy%252Funknown.png%3Falt%3Dmedia%26token%3D18bcff85-b67c-4bbe-a961-28f5c5e58ce3&width=768&dpr=4&quality=100&sign=60c3037c&sv=2)

1. Once it says "successfully loaded model," you can start chatting with the model. Et Voila, you now have an LLM running natively on your Android phone!
![](https://docs.unsloth.ai/~gitbook/image?url=https%3A%2F%2F3215535692-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FxhOjnexMCB3dmuQFQ2Zq%252Fuploads%252FRoYe3aDedHoovwfPJVOh%252Funknown.png%3Falt%3Dmedia%26token%3De9a2cc0a-2407-4c0b-adf1-6e2ba122212c&width=768&dpr=4&quality=100&sign=6adf209b&sv=2)

ExecuTorch [powers on-device ML experiences for billions of people](https://engineering.fb.com/2025/07/28/android/executorch-on-device-ml-meta-family-of-apps/) on Instagram, WhatsApp, Messenger, and Facebook. Instagram Cutouts uses ExecuTorch to extract editable stickers from photos. In encrypted applications like Messenger, ExecuTorch enables on-device privacy aware language identification and translation. ExecuTorch supports over a dozen hardware backends across Apple, Qualcomm, ARM and [Meta’s Quest 3 and Ray Bans](https://ai.meta.com/blog/executorch-reality-labs-on-device-ai/).

- All Qwen 3 dense models ([Qwen3-0.6B](https://huggingface.co/unsloth/Qwen3-0.6B), [Qwen3-4B](https://huggingface.co/unsloth/Qwen3-4B), [Qwen3-32B](https://huggingface.co/unsloth/Qwen3-32B) etc)
- All Gemma 3 models ([Gemma3-270M](https://huggingface.co/unsloth/gemma-3-270m-it), [Gemma3-4B](https://huggingface.co/unsloth/gemma-3-4b-it), [Gemma3-27B](https://huggingface.co/unsloth/gemma-3-27b-it) etc)
- All Llama 3 models ([Llama 3.1 8B](https://huggingface.co/unsloth/Llama-3.1-8B-Instruct), [Llama 3.3 70B Instruct](https://huggingface.co/unsloth/Llama-3.3-70B-Instruct) etc)
- Qwen 2.5, Phi 4 Mini models, and much more!

You can customize the [**free Colab notebook**](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen3_\(0_6B\)-Phone_Deployment.ipynb) for Qwen3-0.6B to allow phone deployment for any of the models above!

**Qwen3 0.6B main phone deployment notebook**

[Google Colab colab.research.google.com](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Qwen3_\(0_6B\)-Phone_Deployment.ipynb)

Works with Gemma 3

[Google Colab colab.research.google.com](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Gemma3_\(4B\).ipynb)

Works with Llama 3

[Google Colab colab.research.google.com](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Llama3.2_\(1B_and_3B\)-Conversational.ipynb)

Go to our [Unsloth Notebooks](https://docs.unsloth.ai/get-started/unsloth-notebooks) page for all other notebooks!

[Previous DPO, ORPO, KTO](https://docs.unsloth.ai/get-started/reinforcement-learning-rl-guide/reinforcement-learning-dpo-orpo-and-kto) [Next New 3x Faster Training](https://docs.unsloth.ai/new/3x-faster-training-packing)

Last updated

Was this helpful?