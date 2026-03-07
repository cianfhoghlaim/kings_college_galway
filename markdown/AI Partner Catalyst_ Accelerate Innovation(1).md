---
title: "AI Partner Catalyst: Accelerate Innovation"
source: "https://ai-partner-catalyst.devpost.com/resources"
author:
  - "[[AI Partner Catalyst: Accelerate Innovation]]"
published:
created: 2025-12-20
description: "Accelerating innovation through the Google Cloud partner ecosystem"
tags:
  - "clippings"
---
#### GOOGLE CLOUD

- Access to Google Cloud may be obtained by signing up for a no cost trial at [https://cloud.google.com/free](https://cloud.google.com/free)

#### DATADOG

**Datadog Challenge**

Using Datadog, implement an innovative end-to-end observability monitoring strategy for an LLM application (new or reused) of your choice, powered by Vertex AI or Gemini. Stream LLM and runtime telemetry to Datadog, define detection rules, and present a clear dashboard that surfaces application health and the observability/security signals you consider essential. When any detection rule is triggered, leverage Datadog to define an actional item (e.g., case, incident, alert, etc.) with context for an AI engineer to act on.

You have full access to Datadog, be creative in how you leverage the platform and the telemetry you emit.

[Join our webinar on December 9th](https://www.datadoghq.com/partner/gcpaihackathon/) - 9:00-9:45am EST to access an additional 30 days of our free trial!

**Hard requirements**

- Provide an in-Datadog view that clearly shows your application health (e.g. latency/errors/tokens/cost), SLOs, and actionable items from the detection rules you defined.
- Create an actionable record inside Datadog with clear contextual information to drive next steps. (Incident Management or Case Management)
- Applications must leverage Vertex AI or Gemini as the model host.
- Report your application telemetry data to Datadog (e.g., LLM observability signals, APM, logs, infrastructure metrics, RUM, etc.) using your preferred method (auto-instrumentation, SDKs, or API).
- Define at least 3 detection rules in Datadog to evaluate application signals and determine when the app requires attention (e.g., monitors/SLOs).
- Create an actionable record inside Datadog with clear contextual information to drive next steps (E.g. Signal Data, Runbook, context around the signal).
- Provide an in-Datadog view that clearly shows application health from the relevant signals collected, detection rules status and any actional items status derived from your detection rules.

**What to Submit**

- Hosted application URL
- Public repo with:
- Approved [OSI license](https://opensource.org/licenses)
	- The instrumented LLM application
	- README with deployment instructions to run the application.
	- JSON export with the Datadog configurations you made (monitors, SLOs, dashboards, etc.)
	- Name the Datadog organization you configured.
	- Traffic generator: A script that generates interactions with your application and demonstrates detection rules in action.
- 3-minute video walkthrough of the solution:
- Explain your observability strategy, the thought process behind your detection rules, what sets you apart from an innovation perspective and any challenges you faced.
- Evidence of your strategy:
- Functioning dashboard link/screenshots
	- Criteria and rationale behind the detection rules configured
	- Incident example: Screenshots showing symptoms, what tripped, timeframe, actionable item creation from detection rule, etc.

**Resources & Support**

- Full access to [Datadog for 14 days](https://www.datadoghq.com/dg/monitor/free-trial/?utm_source=google&utm_medium=paid-search&utm_campaign=dg-brand-ww&utm_keyword=datadog%20trial&utm_matchtype=p&igaag=95325237782&igaat=&igacm=9551169254&igacr=651959074277&igakw=datadog%20trial&igamt=p&igant=g&utm_campaignid=9551169254&utm_adgroupid=95325237782&gad_source=1&gad_campaignid=9551169254). (Trial Account)
- [Datadog Documentation](https://docs.datadoghq.com/) \- product docs and how-to guides.
- [Datadog Learning Center](https://learn.datadoghq.com/) \- self-enablement paths relevant to all Datadog products.
- [Datadog Support](https://www.datadoghq.com/support/) \- help for Datadog-related questions.

#### CONFLUENT

**Confluent Challenge**

Unleash the power of AI on data in motion! Your challenge is to build a next-generation AI application using Confluent and Google Cloud. Apply advanced AI/ML models to any real-time data stream to generate predictions, create dynamic experiences, or solve a compelling problem in a novel way. Demonstrate how real-time data unlocks real-world challenges with AI.

**About Confluent**

Confluent is the cloud-native data streaming platform that sets data in motion, enabling organizations to stream, connect, process, and govern data in real time. Built on Apache Kafka and Flink, Confluent powers mission-critical AI and analytics by delivering trustworthy, contextualized data for intelligent applications and agentic AI. With fully managed services and advanced capabilities like Confluent Intelligence and Streaming Agents, Confluent provides the foundation for building real-time AI systems and unlocking the full potential of enterprise data.

**Resources & Support**

The world is a continuous stream of events. Every mouse click, financial transaction, sensor reading, and log entry is data in motion, happening right now. Your mission is to harness this constant flow of real-time data using the power of Confluent's data streaming platform and Google Cloud's AI capabilities. Move beyond analyzing data at rest and start building applications that react, predict, and adapt the moment an event occurs.

- For questions or assistance please contact: [gcpteam@confluent.io](https://ai-partner-catalyst.devpost.com/)
- Here’s your 30-day Confluent Cloud trial code to activate your trial period: CONFLUENTDEV1

**Examples**

- Dynamic Pricing: Instantly adjust e-commerce prices based on competitor actions and surging real-time demand.
- Fraud Detection: Block suspicious transactions before they are completed, not after.
- Predictive Maintenance: Identify potential IoT failures from a live stream of sensor data, enabling proactive service.
- Hyper-Personalized Gaming: An AI Dungeon Master adapts the story and environment in real-time based on your every move.

Show us - with your innovation - how you can shape the future, as it happens, by building a next-generation application powered by AI on data in motion!

**Documentation:**

- [Build AI with Confluent](https://docs.confluent.io/cloud/current/ai/overview.html): Learn how to use Flink SQL for integration with built-in AI/ML and search functions, and connect Streaming Agents with platforms like Vertex AI.
- [Developer Learning Hub](https://developer.confluent.io/): Dive into tutorials, guides, and courses on all things Confluent.
- [Confluent Cloud Documentation](https://docs.confluent.io/cloud/current/overview.html): Official resource for learning how to deploy, manage, and use Confluent Cloud.
- [Confluent’s MCP server](https://github.com/confluentinc/mcp-confluent): Leverage MCP in an AI Workflow.
- [Confluent Connectors](https://docs.confluent.io/cloud/current/connectors/overview.html): Explore the vast library of pre-built connectors to stream data from any source or to any sink.
- [Google Cloud Connectors](https://www.confluent.io/hub/plugins?query=Google): Integrate seamlessly with key Google Cloud services like [BigQuery](https://docs.confluent.io/cloud/current/connectors/cc-gcp-bigquery-storage-sink.html) and .

**Quickstarts:**

- [Generative AI Healthcare Quickstart](https://github.com/confluentinc/gcp-flink-cflt-genai-quickstart/blob/main/README.md): Automate patient pre-screening with a conversational AI assistant using Confluent Cloud, Gemini AI, and BigQuery.
- [Gen AI Chatbot Quickstart](https://github.com/confluentinc/mongodb-cflt-gcp-genai-quickstart/blob/main/README.md): Empower medical professionals with an intelligent chatbot for medication data access, enhanced by RAG with MongoDB's vector database.

**Certification:**

- [Confluent Certification](https://www.confluent.io/certification/): Validate your Apache Kafka expertise with a Confluent Certification.

#### ELEVENLABS

**ElevenLabs Challenge**

Use ElevenLabs and Google Cloud AI to make your app conversational, intelligent, and voice-driven.

Combine ElevenLabs Agents with Google Cloud Vertex AI or Gemini to give your app a natural, human voice and personality — enabling users to interact entirely through speech. You can integrate ElevenLabs’ APIs directly into your app using our React SDK or via server-side calls hosted on Google Cloud.

**Examples**

- Voice-enabled chatbots powered by Gemini and ElevenLabs
- Interactive video or media experiences built on GCP with real-time narration
- AI-driven customer service or sales agents running on Vertex AI + ElevenLabs

**Resources & Support**

- [ElevenLabs Docs](https://elevenlabs.io/docs/overview) for quickstart and tutorials
- [Conversational AI quickstart guide](https://elevenlabs.io/docs/conversational-ai/quickstart)
	- [Conversational Voice Design](https://elevenlabs.io/docs/conversational-ai/best-practices/conversational-voice-design)
	- [Prompting Guide](https://elevenlabs.io/docs/conversational-ai/best-practices/prompting-guide)
- Google Cloud Vertex AI & Gemini documentation
- Join the [ElevenLabs Discord](https://discord.com/invite/elevenlabs) for live support

**Tips from a Judge**

- Thor from ElevenLabs recommends exploring our Conversational AI and Voice Design Best Practices sections in the docs to create lifelike, engaging voice experiences.

## No conversations yet

Head towards the [Participants tab](https://ai-partner-catalyst.devpost.com/participants) to find teammates, and start conversations by clicking the "Message" button.

P.S. Ensure your status is set to Looking for teammates.

[Messaging](https://ai-partner-catalyst.devpost.com/#)