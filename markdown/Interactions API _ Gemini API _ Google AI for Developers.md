---
title: "Interactions API  |  Gemini API  |  Google AI for Developers"
source: "https://ai.google.dev/gemini-api/docs/interactions?ua=deep-research"
author:
published:
created: 2025-12-21
description: "Get started building with the next-generation Gemini API."
tags:
  - "clippings"
---
The Interactions API is a unified interface for interacting with Gemini models and agents. It simplifies state management, tool orchestration, and long-running tasks. For comprehensive view of the API schema, see the [API Reference](https://ai.google.dev/api/interactions-api).

The following example uses the Interactions API with the [Deep Research agent](https://ai.google.dev/gemini-api/docs/deep-research) to start a research task about the history of Google TPUs and poll for results.

```
import time
from google import genai

client = genai.Client()

# 1. Start the Deep Research Agent
initial_interaction = client.interactions.create(
    input="Research the history of the Google TPUs with a focus on 2025 and 2026.",
    agent="deep-research-pro-preview-12-2025",
    background=True
)

print(f"Research started. Interaction ID: {initial_interaction.id}")

# 2. Poll for results
while True:
    interaction = client.interactions.get(initial_interaction.id)
    print(f"Status: {interaction.status}")

    if interaction.status == "completed":
        print("\nFinal Report:\n", interaction.outputs[-1].text)
        break
    elif interaction.status in ["failed", "completed"]:
        print(f"Failed with status: {interaction.status}")
        break

    time.sleep(10)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

// 1. Start the Deep Research Agent
const initialInteraction =  await client.interactions.create({
    input: 'Research the history of the Google TPUs with a focus on 2025 and 2026.',
    agent: 'deep-research-pro-preview-12-2025',
    background: true
});

console.log(\`Research started. Interaction ID: ${initialInteraction.id}\`);

// 2. Poll for results
while (true) {
    const interaction = await client.interactions.get(initialInteraction.id);
    console.log(\`Status: ${interaction.status}\`);

    if (interaction.status === 'completed') {
        console.log('\nFinal Report:\n', interaction.outputs[interaction.outputs.length - 1].text);
        break;
    } else if (['failed', 'cancelled'].includes(interaction.status)) {
        console.log(\`Failed with status: ${interaction.status}\`);
        break;
    }

    await new Promise(resolve => setTimeout(resolve, 10000));
}
```
```
# 1. Start the Deep Research Agent
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "input": "Research the history of the Google TPUs with a focus on 2025 and 2026.",
    "agent": "deep-research-pro-preview-12-2025",
    "background": true
}'

# 2. Poll for results (Replace INTERACTION_ID with the ID from the previous interaction)
# curl -X GET "https://generativelanguage.googleapis.com/v1beta/interactions/INTERACTION_ID" \
# -H "x-goog-api-key: $GEMINI_API_KEY"
```

The Interactions API is available through our [existing SDKs](https://ai.google.dev/gemini-api/docs/?ua=deep-research#sdk). The simplest way to interact with the model is by providing a text prompt. The `input` can be a string, a list containing a content objects, or a list of turns with roles and content objects.

```
from google import genai

client = genai.Client()

interaction =  client.interactions.create(
    model="gemini-3-flash-preview",
    input="Tell me a short joke about programming."
)

print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const interaction =  await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Tell me a short joke about programming.',
});

console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Tell me a short joke about programming."
}'
```

## Conversation

You can build multi-turn conversations in two ways:

- Statefully by referencing a previous interaction
- Statelessly by providing the entire conversation history

### Stateful conversation

Pass the `id` from the previous interaction to the `previous_interaction_id` parameter to continue a conversation.

```
from google import genai

client = genai.Client()

# 1. First turn
interaction1 = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Hi, my name is Phil."
)
print(f"Model: {interaction1.outputs[-1].text}")

# 2. Second turn (passing previous_interaction_id)
interaction2 = client.interactions.create(
    model="gemini-3-flash-preview",
    input="What is my name?",
    previous_interaction_id=interaction1.id
)
print(f"Model: {interaction2.outputs[-1].text}")
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

// 1. First turn
const interaction1 = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Hi, my name is Phil.'
});
console.log(\`Model: ${interaction1.outputs[interaction1.outputs.length - 1].text}\`);

// 2. Second turn (passing previous_interaction_id)
const interaction2 = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'What is my name?',
    previous_interaction_id: interaction1.id
});
console.log(\`Model: ${interaction2.outputs[interaction2.outputs.length - 1].text}\`);
```
```
# 1. First turn
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Hi, my name is Phil."
}'

# 2. Second turn (Replace INTERACTION_ID with the ID from the previous interaction)
# curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
# -H "Content-Type: application/json" \
# -H "x-goog-api-key: $GEMINI_API_KEY" \
# -d '{
#     "model": "gemini-3-flash-preview",
#     "input": "What is my name?",
#     "previous_interaction_id": "INTERACTION_ID"
# }'
```

Using the interaction `id` to retrieve previous turns of the conversation.

```
previous_interaction = client.interactions.get("<YOUR_INTERACTION_ID>")

print(previous_interaction)
```
```
const previous_interaction = await client.interactions.get("<YOUR_INTERACTION_ID>");
console.log(previous_interaction);
```
```
curl -X GET "https://generativelanguage.googleapis.com/v1beta/interactions/<YOUR_INTERACTION_ID>" \
-H "x-goog-api-key: $GEMINI_API_KEY"
```

### Stateless conversation

You can manage conversation history manually on the client side.

```
from google import genai

client = genai.Client()

conversation_history = [
    {
        "role": "user",
        "content": "What are the three largest cities in Spain?"
    }
]

interaction1 = client.interactions.create(
    model="gemini-3-flash-preview",
    input=conversation_history
)

print(f"Model: {interaction1.outputs[-1].text}")

conversation_history.append({"role": "model", "content": interaction1.outputs})
conversation_history.append({
    "role": "user", 
    "content": "What is the most famous landmark in the second one?"
})

interaction2 = client.interactions.create(
    model="gemini-3-flash-preview",
    input=conversation_history
)

print(f"Model: {interaction2.outputs[-1].text}")
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const conversationHistory = [
    {
        role: 'user',
        content: "What are the three largest cities in Spain?"
    }
];

const interaction1 = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: conversationHistory
});

console.log(\`Model: ${interaction1.outputs[interaction1.outputs.length - 1].text}\`);

conversationHistory.push({ role: 'model', content: interaction1.outputs });
conversationHistory.push({
    role: 'user',
    content: "What is the most famous landmark in the second one?"
});

const interaction2 = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: conversationHistory
});

console.log(\`Model: ${interaction2.outputs[interaction2.outputs.length - 1].text}\`);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
 -H "Content-Type: application/json" \
 -H "x-goog-api-key: $GEMINI_API_KEY" \
 -d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {
            "role": "user",
            "content": "What are the three largest cities in Spain?"
        },
        {
            "role": "model",
            "content": "The three largest cities in Spain are Madrid, Barcelona, and Valencia."
        },
        {
            "role": "user",
            "content": "What is the most famous landmark in the second one?"
        }
    ]
}'
```

## Multimodal capabilities

You can use the Interactions API for multimodal use cases such as image understanding or video generation.

### Multimodal understanding

You can provide multimodal data as base64 encoded data inline or using the Files API for larger files.

#### Image understanding

```
import base64
from pathlib import Path
from google import genai

client = genai.Client()

# Read and encode the image
with open(Path(__file__).parent / "car.png", "rb") as f:
    base64_image = base64.b64encode(f.read()).decode('utf-8')

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=[
        {"type": "text", "text": "Describe the image."},
        {"type": "image", "data": base64_image, "mime_type": "image/png"}
    ]
)

print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';

const client = new GoogleGenAI({});

const base64Image = fs.readFileSync('car.png', { encoding: 'base64' });

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: [
        { type: 'text', text: 'Describe the image.' },
        { type: 'image', data: base64Image, mime_type: 'image/png' }
    ]
});

console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {"type": "text", "text": "Describe the image."},
        {"type": "image", "data": "'"$(base64 -w0 car.png)"'", "mime_type": "image/png"}
    ]
}'
```

#### Audio understanding

```
import base64
from pathlib import Path
from google import genai

client = genai.Client()

# Read and encode the audio
with open(Path(__file__).parent / "speech.wav", "rb") as f:
    base64_audio = base64.b64encode(f.read()).decode('utf-8')

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=[
        {"type": "text", "text": "What does this audio say?"},
        {"type": "audio", "data": base64_audio, "mime_type": "audio/wav"}
    ]
)

print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';

const client = new GoogleGenAI({});

const base64Audio = fs.readFileSync('speech.wav', { encoding: 'base64' });

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: [
        { type: 'text', text: 'What does this audio say?' },
        { type: 'audio', data: base64Audio, mime_type: 'audio/wav' }
    ]
});

console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {"type": "text", "text": "What does this audio say?"},
        {"type": "audio", "data": "'"$(base64 -w0 speech.wav)"'", "mime_type": "audio/wav"}
    ]
}'
```

#### Video understanding

```
import base64
from pathlib import Path
from google import genai

client = genai.Client()

# Read and encode the video
with open(Path(__file__).parent / "video.mp4", "rb") as f:
    base64_video = base64.b64encode(f.read()).decode('utf-8')

print("Analyzing video...")
interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=[
        {"type": "text", "text": "What is happening in this video? Provide a timestamped summary."},
        {"type": "video", "data": base64_video, "mime_type": "video/mp4" }
    ]
)

print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';

const client = new GoogleGenAI({});

const base64Video = fs.readFileSync('video.mp4', { encoding: 'base64' });

console.log('Analyzing video...');
const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: [
        { type: 'text', text: 'What is happening in this video? Provide a timestamped summary.' },
        { type: 'video', data: base64Video, mime_type: 'video/mp4'}
    ]
});

console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {"type": "text", "text": "What is happening in this video?"},
        {"type": "video", "mime_type": "video/mp4", "data": "'"$(base64 -w0 video.mp4)"'"}
    ]
}'
```

#### Document (PDF) understanding

```
import base64
from google import genai

client = genai.Client()

with open("sample.pdf", "rb") as f:
    base64_pdf = base64.b64encode(f.read()).decode('utf-8')

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=[
        {"type": "text", "text": "What is this document about?"},
        {"type": "document", "data": base64_pdf, "mime_type": "application/pdf"}
    ]
)
print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';
const client = new GoogleGenAI({});

const base64Pdf = fs.readFileSync('sample.pdf', { encoding: 'base64' });

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: [
        { type: 'text', text: 'What is this document about?' },
        { type: 'document', data: base64Pdf, mime_type: 'application/pdf' }
    ],
});
console.log(interaction.outputs[0].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {"type": "text", "text": "What is this document about?"},
        {"type": "document", "data": "'"$(base64 -w0 sample.pdf)"'", "mime_type": "application/pdf"}
    ]
}'
```

### Multimodal generation

You can use Interactions API to generation multimodal outputs.

#### Image generation

```
import base64
from google import genai

client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3-pro-image-preview",
    input="Generate an image of a futuristic city.",
    response_modalities=["IMAGE"]
)

for output in interaction.outputs:
    if output.type == "image":
        print(f"Generated image with mime_type: {output.mime_type}")
        # Save the image
        with open("generated_city.png", "wb") as f:
            f.write(base64.b64decode(output.data))
```
```
import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';

const client = new GoogleGenAI({});

const interaction = await client.interactions.create({
    model: 'gemini-3-pro-image-preview',
    input: 'Generate an image of a futuristic city.',
    response_modalities: ['IMAGE']
});

for (const output of interaction.outputs) {
    if (output.type === 'image') {
        console.log(\`Generated image with mime_type: ${output.mime_type}\`);
        // Save the image
        fs.writeFileSync('generated_city.png', Buffer.from(output.data, 'base64'));
    }
}
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-pro-image-preview",
    "input": "Generate an image of a futuristic city.",
    "response_modalities": ["IMAGE"]
}'
```

## Agentic capabilities

The Interactions API is designed for building and interacting with agents, and includes support for function calling, built-in tools, structured outputs, and the Model Context Protocol (MCP).

### Agents

You can use specialized agents like `deep-research-pro-preview-12-2025` for complex tasks. To learn more about the Gemini Deep Research Agent, see the [Deep Research](https://ai.google.dev/gemini-api/docs/deep-research) guide.

```
import time
from google import genai

client = genai.Client()

# 1. Start the Deep Research Agent
initial_interaction = client.interactions.create(
    input="Research the history of the Google TPUs with a focus on 2025 and 2026.",
    agent="deep-research-pro-preview-12-2025",
    background=True
)

print(f"Research started. Interaction ID: {initial_interaction.id}")

# 2. Poll for results
while True:
    interaction = client.interactions.get(initial_interaction.id)
    print(f"Status: {interaction.status}")

    if interaction.status == "completed":
        print("\nFinal Report:\n", interaction.outputs[-1].text)
        break
    elif interaction.status in ["failed", "cancelled"]:
        print(f"Failed with status: {interaction.status}")
        break

    time.sleep(10)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

// 1. Start the Deep Research Agent
const initialInteraction = await client.interactions.create({
    input: 'Research the history of the Google TPUs with a focus on 2025 and 2026.',
    agent: 'deep-research-pro-preview-12-2025',
    background: true
});

console.log(\`Research started. Interaction ID: ${initialInteraction.id}\`);

// 2. Poll for results
while (true) {
    const interaction = await client.interactions.get(initialInteraction.id);
    console.log(\`Status: ${interaction.status}\`);

    if (interaction.status === 'completed') {
        console.log('\nFinal Report:\n', interaction.outputs[interaction.outputs.length - 1].text);
        break;
    } else if (['failed', 'cancelled'].includes(interaction.status)) {
        console.log(\`Failed with status: ${interaction.status}\`);
        break;
    }

    await new Promise(resolve => setTimeout(resolve, 10000));
}
```
```
# 1. Start the Deep Research Agent
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "input": "Research the history of the Google TPUs with a focus on 2025 and 2026.",
    "agent": "deep-research-pro-preview-12-2025",
    "background": true
}'

# 2. Poll for results (Replace INTERACTION_ID with the ID from the previous interaction)
# curl -X GET "https://generativelanguage.googleapis.com/v1beta/interactions/INTERACTION_ID" \
# -H "x-goog-api-key: $GEMINI_API_KEY"
```

### Tools and function calling

This section explains how to use function calling to define custom tools and how to use Google's built-in tools within the Interactions API.

#### Function calling

```
from google import genai

client = genai.Client()

# 1. Define the tool
def get_weather(location: str):
    """Gets the weather for a given location."""
    return f"The weather in {location} is sunny."

weather_tool = {
    "type": "function",
    "name": "get_weather",
    "description": "Gets the weather for a given location.",
    "parameters": {
        "type": "object",
        "properties": {
            "location": {"type": "string", "description": "The city and state, e.g. San Francisco, CA"}
        },
        "required": ["location"]
    }
}

# 2. Send the request with tools
interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="What is the weather in Paris?",
    tools=[weather_tool]
)

# 3. Handle the tool call
for output in interaction.outputs:
    if output.type == "function_call":
        print(f"Tool Call: {output.name}({output.arguments})")
        # Execute tool
        result = get_weather(**output.arguments)

        # Send result back
        interaction = client.interactions.create(
            model="gemini-3-flash-preview",
            previous_interaction_id=interaction.id,
            input=[{
                "type": "function_result",
                "name": output.name,
                "call_id": output.id,
                "result": result
            }]
        )
        print(f"Response: {interaction.outputs[-1].text}")
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

// 1. Define the tool
const weatherTool = {
    type: 'function',
    name: 'get_weather',
    description: 'Gets the weather for a given location.',
    parameters: {
        type: 'object',
        properties: {
            location: { type: 'string', description: 'The city and state, e.g. San Francisco, CA' }
        },
        required: ['location']
    }
};

// 2. Send the request with tools
let interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'What is the weather in Paris?',
    tools: [weatherTool]
});

// 3. Handle the tool call
for (const output of interaction.outputs) {
    if (output.type === 'function_call') {
        console.log(\`Tool Call: ${output.name}(${JSON.stringify(output.arguments)})\`);

        // Execute tool (Mocked)
        const result = \`The weather in ${output.arguments.location} is sunny.\`;

        // Send result back
        interaction = await client.interactions.create({
            model: 'gemini-3-flash-preview',
            previous_interaction_id:interaction.id,
            input: [{
                type: 'function_result',
                name: output.name,
                call_id: output.id,
                result: result
            }]
        });
        console.log(\`Response: ${interaction.outputs[interaction.outputs.length - 1].text}\`);
    }
}
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "What is the weather in Paris?",
    "tools": [{
        "type": "function",
        "name": "get_weather",
        "description": "Gets the weather for a given location.",
        "parameters": {
            "type": "object",
            "properties": {
                "location": {"type": "string", "description": "The city and state, e.g. San Francisco, CA"}
            },
            "required": ["location"]
        }
    }]
}'

# Handle the tool call and send result back (Replace INTERACTION_ID and CALL_ID)
# curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
# -H "Content-Type: application/json" \
# -H "x-goog-api-key: $GEMINI_API_KEY" \
# -d '{
#     "model": "gemini-3-flash-preview",
#     "previous_interaction_id": "INTERACTION_ID",
#     "input": [{
#         "type": "function_result",
#         "name": "get_weather",
#         "call_id": "FUNCTION_CALL_ID",
#         "result": "The weather in Paris is sunny."
#     }]
# }'
```

##### Function calling with client-side state

If you don't want to use server-side state, you can manage it all on the client side.

```
from google import genai
client = genai.Client()

functions = [
    {
        "type": "function",
        "name": "schedule_meeting",
        "description": "Schedules a meeting with specified attendees at a given time and date.",
        "parameters": {
            "type": "object",
            "properties": {
                "attendees": {"type": "array", "items": {"type": "string"}},
                "date": {"type": "string", "description": "Date of the meeting (e.g., 2024-07-29)"},
                "time": {"type": "string", "description": "Time of the meeting (e.g., 15:00)"},
                "topic": {"type": "string", "description": "The subject of the meeting."},
            },
            "required": ["attendees", "date", "time", "topic"],
        },
    }
]

history = [{"role": "user","content": [{"type": "text", "text": "Schedule a meeting for 2025-11-01 at 10 am with Peter and Amir about the Next Gen API."}]}]

# 1. Model decides to call the function
interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=history,
    tools=functions
)

# add model interaction back to history
history.append({"role": "model", "content": interaction.outputs})

for output in interaction.outputs:
    if output.type == "function_call":
        print(f"Function call: {output.name} with arguments {output.arguments}")

        # 2. Execute the function and get a result
        # In a real app, you would call your function here.
        # call_result = schedule_meeting(**json.loads(output.arguments))
        call_result = "Meeting scheduled successfully."

        # 3. Send the result back to the model
        history.append({"role": "user", "content": [{"type": "function_result", "name": output.name, "call_id": output.id, "result": call_result}]})

        interaction2 = client.interactions.create(
            model="gemini-3-flash-preview",
            input=history,
        )
        print(f"Final response: {interaction2.outputs[-1].text}")
    else:
        print(f"Output: {output}")
```
```
// 1. Define the tool
const functions = [
    {
        type: 'function',
        name: 'schedule_meeting',
        description: 'Schedules a meeting with specified attendees at a given time and date.',
        parameters: {
            type: 'object',
            properties: {
                attendees: { type: 'array', items: { type: 'string' } },
                date: { type: 'string', description: 'Date of the meeting (e.g., 2024-07-29)' },
                time: { type: 'string', description: 'Time of the meeting (e.g., 15:00)' },
                topic: { type: 'string', description: 'The subject of the meeting.' },
            },
            required: ['attendees', 'date', 'time', 'topic'],
        },
    },
];

const history = [
    { role: 'user', content: [{ type: 'text', text: 'Schedule a meeting for 2025-11-01 at 10 am with Peter and Amir about the Next Gen API.' }] }
];

// 2. Model decides to call the function
let interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: history,
    tools: functions
});

// add model interaction back to history
history.push({ role: 'model', content: interaction.outputs });

for (const output of interaction.outputs) {
    if (output.type === 'function_call') {
        console.log(\`Function call: ${output.name} with arguments ${JSON.stringify(output.arguments)}\`);

        // 3. Send the result back to the model
        history.push({ role: 'user', content: [{ type: 'function_result', name: output.name, call_id: output.id, result: 'Meeting scheduled successfully.' }] });

        const interaction2 = await client.interactions.create({
            model: 'gemini-3-flash-preview',
            input: history,
        });
        console.log(\`Final response: ${interaction2.outputs[interaction2.outputs.length - 1].text}\`);
    }
}
```

#### Built-in tools

Gemini comes with built-in tools like [Grounding with Google Search](https://ai.google.dev/gemini-api/docs/google-search),[Code execution](https://ai.google.dev/gemini-api/docs/code-execution), and [URL context](https://ai.google.dev/gemini-api/docs/url-context).

```
from google import genai

client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Who won the last Super Bowl?",
    tools=[{"type": "google_search"}]
)
# Find the text output (not the GoogleSearchResultContent)
text_output = next((o for o in interaction.outputs if o.type == "text"), None)
if text_output:
    print(text_output.text)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Who won the last Super Bowl?',
    tools: [{ type: 'google_search' }]
});
// Find the text output (not the GoogleSearchResultContent)
const textOutput = interaction.outputs.find(o => o.type === 'text');
if (textOutput) console.log(textOutput.text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Who won the last Super Bowl?",
    "tools": [{"type": "google_search"}]
}'
```

##### Code execution

```
from google import genai

client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Calculate the 50th Fibonacci number.",
    tools=[{"type": "code_execution"}]
)
print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Calculate the 50th Fibonacci number.',
    tools: [{ type: 'code_execution' }]
});
console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Calculate the 50th Fibonacci number.",
    "tools": [{"type": "code_execution"}]
}'
```

##### URL context

```
from google import genai

client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Summarize the content of https://www.wikipedia.org/",
    tools=[{"type": "url_context"}]
)
# Find the text output (not the URLContextResultContent)
text_output = next((o for o in interaction.outputs if o.type == "text"), None)
if text_output:
    print(text_output.text)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Summarize the content of https://www.wikipedia.org/',
    tools: [{ type: 'url_context' }]
});
// Find the text output (not the URLContextResultContent)
const textOutput = interaction.outputs.find(o => o.type === 'text');
if (textOutput) console.log(textOutput.text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Summarize the content of https://www.wikipedia.org/",
    "tools": [{"type": "url_context"}]
}'
```

### Remote Model context protocol (MCP)

Remote [MCP](https://modelcontextprotocol.io/docs/getting-started/intro) integration simplifies agent development by allowing the Gemini API to directly call external tools hosted on remote servers.

```
import datetime
from google import genai

client = genai.Client()

mcp_server = {
    "type": "mcp_server",
    "name": "weather_service",
    "url": "https://gemini-api-demos.uc.r.appspot.com/mcp"
}

today = datetime.date.today().strftime("%d %B %Y")

interaction = client.interactions.create(
    model="gemini-2.5-flash",
    input="What is the weather like in New York today?",
    tools=[mcp_server],
    system_instruction=f"Today is {today}."
)

print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const mcpServer = {
    type: 'mcp_server',
    name: 'weather_service',
    url: 'https://gemini-api-demos.uc.r.appspot.com/mcp'
};

const today = new Date().toDateString();

const interaction = await client.interactions.create({
    model: 'gemini-2.5-flash',
    input: 'What is the weather like in New York today?',
    tools: [mcpServer],
    system_instruction: \`Today is ${today}.\`
});

console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-2.5-flash",
    "input": "What is the weather like in New York today?",
    "tools": [{
        "type": "mcp_server",
        "name": "weather_service",
        "url": "https://gemini-api-demos.uc.r.appspot.com/mcp"
    }],
    "system_instruction": "Today is '"$(date +"%du%Bt%Y")"' YYYY-MM-DD>."
}'
```

**Important notes:**

- Remote MCP only works with Streamable HTTP servers (SSE servers are not supported)
- Remote MCP does not work with Gemini 3 models (this is coming soon)
- MCP server names shouldn't include "-" character (use snake\_case server names instead)

### Structured output (JSON schema)

Enforce a specific JSON output by providing a JSON schema in the `response_format` parameter. This is useful for tasks like moderation, classification, or data extraction.

```
from google import genai
from pydantic import BaseModel, Field
from typing import Literal, Union
client = genai.Client()

class SpamDetails(BaseModel):
    reason: str = Field(description="The reason why the content is considered spam.")
    spam_type: Literal["phishing", "scam", "unsolicited promotion", "other"]

class NotSpamDetails(BaseModel):
    summary: str = Field(description="A brief summary of the content.")
    is_safe: bool = Field(description="Whether the content is safe for all audiences.")

class ModerationResult(BaseModel):
    decision: Union[SpamDetails, NotSpamDetails]

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Moderate the following content: 'Congratulations! You've won a free cruise. Click here to claim your prize: www.definitely-not-a-scam.com'",
    response_format=ModerationResult.model_json_schema(),
)

parsed_output = ModerationResult.model_validate_json(interaction.outputs[-1].text)
print(parsed_output)
```
```
import { GoogleGenAI } from '@google/genai';
import { z } from 'zod';
const client = new GoogleGenAI({});

const moderationSchema = z.object({
    decision: z.union([
        z.object({
            reason: z.string().describe('The reason why the content is considered spam.'),
            spam_type: z.enum(['phishing', 'scam', 'unsolicited promotion', 'other']).describe('The type of spam.'),
        }).describe('Details for content classified as spam.'),
        z.object({
            summary: z.string().describe('A brief summary of the content.'),
            is_safe: z.boolean().describe('Whether the content is safe for all audiences.'),
        }).describe('Details for content classified as not spam.'),
    ]),
});

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: "Moderate the following content: 'Congratulations! You've won a free cruise. Click here to claim your prize: www.definitely-not-a-scam.com'",
    response_format: z.toJSONSchema(moderationSchema),
});
console.log(interaction.outputs[0].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Moderate the following content: 'Congratulations! You've won a free cruise. Click here to claim your prize: www.definitely-not-a-scam.com'",
    "response_format": {
        "type": "object",
        "properties": {
            "decision": {
                "type": "object",
                "properties": {
                    "reason": {"type": "string", "description": "The reason why the content is considered spam."},
                    "spam_type": {"type": "string", "description": "The type of spam."}
                },
                "required": ["reason", "spam_type"]
            }
        },
        "required": ["decision"]
    }
}'
```

### Combining tools and structured output

Combine built-in tools with structured output to get a reliable JSON object based on information retrieved by a tool.

```
from google import genai
from pydantic import BaseModel, Field
from typing import Literal, Union

client = genai.Client()

class SpamDetails(BaseModel):
    reason: str = Field(description="The reason why the content is considered spam.")
    spam_type: Literal["phishing", "scam", "unsolicited promotion", "other"]

class NotSpamDetails(BaseModel):
    summary: str = Field(description="A brief summary of the content.")
    is_safe: bool = Field(description="Whether the content is safe for all audiences.")

class ModerationResult(BaseModel):
    decision: Union[SpamDetails, NotSpamDetails]

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Moderate the following content: 'Congratulations! You've won a free cruise. Click here to claim your prize: www.definitely-not-a-scam.com'",
    response_format=ModerationResult.model_json_schema(),
    tools=[{"type": "url_context"}]
)

parsed_output = ModerationResult.model_validate_json(interaction.outputs[-1].text)
print(parsed_output)
```
```
import { GoogleGenAI } from '@google/genai';
import { z } from 'zod'; // Assuming zod is used for schema generation, or define manually
const client = new GoogleGenAI({});

const obj = z.object({
    winning_team: z.string(),
    score: z.string(),
});
const schema = z.toJSONSchema(obj);

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Who won the last euro?',
    tools: [{ type: 'google_search' }],
    response_format: schema,
});
console.log(interaction.outputs[0].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Who won the last euro?",
    "tools": [{"type": "google_search"}],
    "response_format": {
        "type": "object",
        "properties": {
            "winning_team": {"type": "string"},
            "score": {"type": "string"}
        }
    }
}'
```

## Advanced features

There are also additional advance features that give you more flexibility in working with Interactions API.

### Streaming

Receive responses incrementally as they are generated.

### Configuration

Customize the model's behavior with `generation_config`.

```
from google import genai

client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input="Tell me a story about a brave knight.",
    generation_config={
        "temperature": 0.7,
        "max_output_tokens": 500,
        "thinking_level": "low",
    }
)

print(interaction.outputs[-1].text)
```
```
import { GoogleGenAI } from '@google/genai';

const client = new GoogleGenAI({});

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: 'Tell me a story about a brave knight.',
    generation_config: {
        temperature: 0.7,
        max_output_tokens: 500,
        thinking_level: 'low',
    }
});

console.log(interaction.outputs[interaction.outputs.length - 1].text);
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": "Tell me a story about a brave knight.",
    "generation_config": {
        "temperature": 0.7,
        "max_output_tokens": 500,
        "thinking_level": "low"
    }
}'
```

The `thinking_level` parameter lets you control the model's reasoning behavior for all Gemini 2.5 and newer models.

| Level | Description | Supported Models |
| --- | --- | --- |
| `minimal` | Matches the "no thinking" setting for most queries. In some cases, models may think very minimally. Minimizes latency and cost. | **Flash Models Only**   (e.g. Gemini 3 Flash) |
| `low` | Light reasoning that prioritises latency and cost savings for simple instruction following and chat. | **All Thinking Models** |
| `medium` | Balanced thinking for most tasks. | **Flash Models Only**   (e.g. Gemini 3 Flash) |
| `high` | **(Default)** Maximizes reasoning depth. The model may take significantly longer to reach a first token, but the output will be more carefully reasoned. | **All Thinking Models** |

### Working with files

#### Working with remote files

Access files using remote URLs directly in the API call.

```
from google import genai
client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=[
        {
            "type": "image",
            "uri": "https://github.com/<github-path>/cats-and-dogs.jpg",
        },
        {"type": "text", "text": "Describe what you see."}
    ],
)
for output in interaction.outputs:
    if output.type == "text":
        print(output.text)
```
```
import { GoogleGenAI } from '@google/genai';
const client = new GoogleGenAI({});

const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: [
        {
            type: 'image',
            uri: 'https://github.com/<github-path>/cats-and-dogs.jpg',
        },
        { type: 'text', text: 'Describe what you see.' }
    ],
});
for (const output of interaction.outputs) {
    if (output.type === 'text') {
        console.log(output.text);
    }
}
```
```
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {
            "type": "image",
            "uri": "https://github.com/<github-path>/cats-and-dogs.jpg"
        },
        {"type": "text", "text": "Describe what you see."}
    ]
}'
```

#### Working with Gemini Files API

Upload files to the Gemini [Files API](https://ai.google.dev/gemini-api/docs/files) before using them.

```
from google import genai
import time
import requests
client = genai.Client()

# 1. Download the file
url = "https://github.com/philschmid/gemini-samples/raw/refs/heads/main/assets/cats-and-dogs.jpg"
response = requests.get(url)
with open("cats-and-dogs.jpg", "wb") as f:
    f.write(response.content)

# 2. Upload to Gemini Files API
file = client.files.upload(file="cats-and-dogs.jpg")

# 3. Wait for processing
while client.files.get(name=file.name).state != "ACTIVE":
    time.sleep(2)

# 4. Use in Interaction
interaction = client.interactions.create(
    model="gemini-3-flash-preview",
    input=[
        {
            "type": "image",
            "uri": file.uri,
        },
        {"type": "text", "text": "Describe what you see."}
    ],
)
for output in interaction.outputs:
    if output.type == "text":
        print(output.text)
```
```
import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';
import fetch from 'node-fetch';
const client = new GoogleGenAI({});

// 1. Download the file
const url = 'https://github.com/philschmid/gemini-samples/raw/refs/heads/main/assets/cats-and-dogs.jpg';
const filename = 'cats-and-dogs.jpg';
const response = await fetch(url);
const buffer = await response.buffer();
fs.writeFileSync(filename, buffer);

// 2. Upload to Gemini Files API
const myfile = await client.files.upload({ file: filename, config: { mimeType: 'image/jpeg' } });

// 3. Wait for processing
while ((await client.files.get({ name: myfile.name })).state !== 'ACTIVE') {
    await new Promise(resolve => setTimeout(resolve, 2000));
}

// 4. Use in Interaction
const interaction = await client.interactions.create({
    model: 'gemini-3-flash-preview',
    input: [
        { type: 'image', uri: myfile.uri, },
        { type: 'text', text: 'Describe what you see.' }
    ],
});
for (const output of interaction.outputs) {
    if (output.type === 'text') {
        console.log(output.text);
    }
}
```
```
# 1. Upload the file (Requires File API setup)
# See https://ai.google.dev/gemini-api/docs/files for details.
# Assume FILE_URI is obtained from the upload step.

curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
-H "Content-Type: application/json" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-d '{
    "model": "gemini-3-flash-preview",
    "input": [
        {"type": "image", "uri": "FILE_URI"},
        {"type": "text", "text": "Describe what you see."}
    ]
}'
```

### Data model

You can learn more about the data model in the [API Reference](https://ai.google.dev/api/interactions-api#data-models). The following is a high level overview of the main components.

| Property | Type | Description |
| --- | --- | --- |
| `id` | `string` | Unique identifier for the interaction. |
| `model` / `agent` | `string` | The model or agent used. Only one can be provided. |
| `input` | [`Content[]`](https://ai.google.dev/api/interactions-api#data-models) | The inputs provided. |
| `outputs` | [`Content[]`](https://ai.google.dev/api/interactions-api#data-models) | The model's responses. |
| `tools` | [`Tool[]`](https://ai.google.dev/api/interactions-api#Resource:Tool) | The tools used. |
| `previous_interaction_id` | `string` | ID of the previous interaction for context. |
| `stream` | `boolean` | Whether the interaction is streaming. |
| `status` | `string` | Status: `completed`, `in_progress`, `requires_action`,`failed`, etc. |
| `background` | `boolean` | Whether the interaction is in background mode. |
| `store` | `boolean` | Whether to store the interaction. Default: `true`. Set to `false` to opt out. |
| `usage` | [Usage](https://ai.google.dev/api/interactions-api#Resource:Interaction) | Token usage of the interaction request. |

## Supported models & agents

| Model Name | Type | Model ID |
| --- | --- | --- |
| Gemini 2.5 Pro | Model | `gemini-2.5-pro` |
| Gemini 2.5 Flash | Model | `gemini-2.5-flash` |
| Gemini 2.5 Flash-lite | Model | `gemini-2.5-flash-lite` |
| Gemini 3 Pro Preview | Model | `gemini-3-pro-preview` |
| Gemini 3 Flash Preview | Model | `gemini-3-flash-preview` |
| Deep Research Preview | Agent | `deep-research-pro-preview-12-2025` |

## How the Interactions API works

The Interactions API is designed around a central resource: the [**`Interaction`**](https://ai.google.dev/api/interactions-api#Resource:Interaction). An `Interaction` represents a complete turn in a conversation or task. It acts as a session record, containing the entire history of an interaction, including all user inputs, model thoughts, tool calls, tool results, and final model outputs.

When you make a call to [`interactions.create`](https://ai.google.dev/api/interactions-api#CreateInteraction), you are creating a new `Interaction` resource.

Optionally, you can use the `id` of this resource in a subsequent call using the `previous_interaction_id` parameter to continue the conversation. The server uses this ID to retrieve the full context, saving you from having to resend the entire chat history. This server-side state management is optional; you can also operate in stateless mode by sending the full conversation history in each request.

### Data storage and retention

By default, all Interaction objects are stored (`store=true`) in order to simplify use of server-side state management features (with `previous_interaction_id`), background execution (using `background=true`) and observability purposes.

- **Paid Tier**: Interactions are retained for **55 days**.
- **Free Tier**: Interactions are retained for **1 day**.

If you do not want this, you can set `store=false` in your request. This control is separate from state management; you can opt out of storage for any interaction. However, note that `store=false` is incompatible with `background=true` and prevents using `previous_interaction_id` for subsequent turns.

You can delete stored interactions at any time using the delete method found in the [API Reference](https://ai.google.dev/api/interactions-api). You can only delete interactions if you know the interaction ID.

After the retention period expires, your data will be deleted automatically.

Interactions objects are processed according to the [terms](https://ai.google.dev/gemini-api/terms).

## Best practices

- **Cache hit rate**: Using `previous_interaction_id` to continue conversations allows the system to more easily utilize implicit caching for the conversation history, which improves performance and reduces costs.
- **Mixing interactions**: You have the flexibility to mix and match Agent and Model interactions within a conversation. For instance, you can use a specialized agent, like the Deep Research agent, for initial data collection, and then use a standard Gemini model for follow-up tasks such as summarizing or reformatting, linking these steps with the `previous_interaction_id`.

## SDKs

You can use latest version of the Google GenAI SDKs in order to access Interactions API.

- On Python, this is `google-genai` package from `1.55.0` version onwards.
- On JavaScript, this is `@google/genai` package from `1.33.0` version onwards.

You can learn more about how to install the SDKs on [Libraries](https://ai.google.dev/gemini-api/docs/libraries) page.

## Limitations

- **Beta status**: The Interactions API is in beta/preview. Features and schemas may change.
- **Unsupported features**: The following features are not yet supported but are coming soon:
	- [Grounding with Google Maps](https://ai.google.dev/gemini-api/docs/maps-grounding)
	- [Computer Use](https://ai.google.dev/gemini-api/docs/computer-use)
- **Output ordering**: Content ordering for built-in tools (`google_search` and `url_context`) may sometimes be incorrect, with text appearing before the tool execution and result. This is a known issue and a fix is in progress.
- **Tool combinations**: Combining MCP, Function Call, and Built-in tools is not yet supported but is coming soon.
- **Remote MCP**: Gemini 3 does not support remote mcp, this is coming soon.

## Breaking changes

The Interactions API is currently in an early beta stage. We are actively developing and refining the API capabilities, resource schemas, and SDK interfaces based on real-world usage and developer feedback.

As a result, **breaking changes may occur**. Updates may include changes to:

- Schemas for input and output.
- SDK method signatures and object structures.
- Specific feature behaviors.

For production workloads, you should continue to use the standard [`generateContent`](https://ai.google.dev/gemini-api/docs/text-generation) API. It remains the recommended path for stable deployments and will continue to be actively developed and maintained.

Your feedback is critical to the development of the Interactions API. Please share your thoughts, report bugs, or request features on our [Google AI Developer Community Forum](https://discuss.ai.google.dev/c/gemini-api/4).

## What's next

- Learn more about the [Gemini Deep Research Agent](https://ai.google.dev/gemini-api/docs/deep-research).

Except as otherwise noted, the content of this page is licensed under the [Creative Commons Attribution 4.0 License](https://creativecommons.org/licenses/by/4.0/), and code samples are licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0). For details, see the [Google Developers Site Policies](https://developers.google.com/site-policies). Java is a registered trademark of Oracle and/or its affiliates.

Last updated 2025-12-19 UTC.

The new page has loaded.