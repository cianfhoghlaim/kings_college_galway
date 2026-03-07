---
title: "Kafka | dlt Docs"
source: "https://dlthub.com/docs/dlt-ecosystem/verified-sources/kafka"
author:
published:
created: 2025-12-20
description: "dlt verified source for Confluent Kafka"
tags:
  - "clippings"
---
Version: 1.20.0 (latest)

[Kafka](https://www.confluent.io/) is an open-source distributed event streaming platform, organized in the form of a log with message publishers and subscribers. The Kafka `dlt` verified source loads data using the Confluent Kafka API to the destination of your choice. See a [pipeline example](https://github.com/dlt-hub/verified-sources/blob/master/sources/kafka_pipeline.py).

The resource that can be loaded:

| Name | Description |
| --- | --- |
| kafka\_consumer | Extracts messages from Kafka topics |

## Setup guide

### Grab Kafka cluster credentials

1. Follow the [Kafka Setup](https://developer.confluent.io/get-started/python/#kafka-setup) to tweak a project.
2. Follow the [Configuration](https://developer.confluent.io/get-started/python/#configuration) to get the project credentials.

### Initialize the verified source

To get started with your data pipeline, follow these steps:

1. Enter the following command:
	```sh
	dlt init kafka duckdb
	```
	[This command](https://dlthub.com/docs/reference/command-line-interface) will initialize [the pipeline example](https://github.com/dlt-hub/verified-sources/blob/master/sources/kafka_pipeline.py) with Kafka as the [source](https://dlthub.com/docs/general-usage/source) and [duckdb](https://dlthub.com/docs/dlt-ecosystem/destinations/duckdb) as the [destination](https://dlthub.com/docs/dlt-ecosystem/destinations).
2. If you'd like to use a different destination, simply replace `duckdb` with the name of your preferred [destination](https://dlthub.com/docs/dlt-ecosystem/destinations).
3. After running this command, a new directory will be created with the necessary files and configuration settings to get started.

For more information, read the [Walkthrough: Add a verified source.](https://dlthub.com/docs/walkthroughs/add-a-verified-source)

### Add credentials

1. In the `.dlt` folder, there's a file called `secrets.toml`. It's where you store sensitive information securely, like access tokens. Keep this file safe.
	Use the following format for service account authentication:
```toml
[sources.kafka.credentials]
bootstrap_servers="web.address.gcp.confluent.cloud:9092"
group_id="test_group"
security_protocol="SASL_SSL"
sasl_mechanisms="PLAIN"
sasl_username="example_username"
sasl_password="example_secret"
```
1. Enter credentials for your chosen destination as per the [docs](https://dlthub.com/docs/dlt-ecosystem/destinations).

## Run the pipeline

1. Before running the pipeline, ensure that you have installed all the necessary dependencies by running the command:
	```sh
	pip install -r requirements.txt
	```
2. You're now ready to run the pipeline! To get started, run the following command:
	```sh
	python kafka_pipeline.py
	```
3. Once the pipeline has finished running, you can verify that everything loaded correctly by using the following command:
	```sh
	dlt pipeline <pipeline_name> show
	```

For more information, read the [Walkthrough: Run a pipeline](https://dlthub.com/docs/walkthroughs/run-a-pipeline).

## Sources and resources

`dlt` works on the principle of [sources](https://dlthub.com/docs/general-usage/source) and [resources](https://dlthub.com/docs/general-usage/resource).

### Source kafka\_consumer

This function retrieves messages from the given Kafka topics.

```markdown
@dlt.resource(name="kafka_messages", table_name=lambda msg: msg["_kafka"]["topic"])
def kafka_consumer(
    topics: Union[str, List[str]],
    credentials: Union[KafkaCredentials, Consumer] = dlt.secrets.value,
    msg_processor: Optional[Callable[[Message], Dict[str, Any]]] = default_msg_processor,
    batch_size: Optional[int] = 3000,
    batch_timeout: Optional[int] = 3,
    start_from: Optional[TAnyDateTime] = None,
) -> Iterable[TDataItem]:
   ...
```

`topics`: A list of Kafka topics to be extracted.

`credentials`: By default, it is initialized with the data from the `secrets.toml`. It may be used explicitly to pass an initialized Kafka Consumer object.

`msg_processor`: A function that will be used to process every message read from the given topics before saving them in the destination. It can be used explicitly to pass a custom processor. See the [default processor](https://github.com/dlt-hub/verified-sources/blob/fe8ed7abd965d9a0ca76d100551e7b64a0b95744/sources/kafka/helpers.py#L14-L50) as an example of how to implement processors.

`batch_size`: The number of messages to extract from the cluster at once. It can be set to tweak performance.

`batch_timeout`: The maximum timeout (in seconds) for a single batch reading operation. It can be set to tweak performance.

`start_from`: A timestamp, starting from which the messages must be read. When passed, `dlt` asks the Kafka cluster for an offset, which is actual for the given timestamp, and starts to read messages from this offset.

## Customization

### Create your own pipeline

1. Configure the pipeline by specifying the pipeline name, destination, and dataset as follows:
	```markdown
	pipeline = dlt.pipeline(
	     pipeline_name="kafka",     # Use a custom name if desired
	     destination="duckdb",      # Choose the appropriate destination (e.g., duckdb, redshift, post)
	     dataset_name="kafka_data"  # Use a custom name if desired
	)
	```
2. To extract several topics:
	```markdown
	topics = ["topic1", "topic2", "topic3"]
	resource = kafka_consumer(topics)
	pipeline.run(resource, write_disposition="replace")
	```
3. To extract messages and process them in a custom way:
	```markdown
	def custom_msg_processor(msg: confluent_kafka.Message) -> Dict[str, Any]:
	     return {
	         "_kafka": {
	             "topic": msg.topic(),  # required field
	             "key": msg.key().decode("utf-8"),
	             "partition": msg.partition(),
	         },
	         "data": msg.value().decode("utf-8"),
	     }
	 resource = kafka_consumer("topic", msg_processor=custom_msg_processor)
	 pipeline.run(resource)
	```
4. To extract messages, starting from a timestamp:
	```markdown
	resource = kafka_consumer("topic", start_from=pendulum.DateTime(2023, 12, 15))
	 pipeline.run(resource)
	```

This demo works on codespaces. Codespaces is a development environment available for free to anyone with a Github account. You'll be asked to fork the demo repository and from there the README guides you with further steps.

The demo uses the Continue VSCode extension.

  
[Off to codespaces!](https://github.com/codespaces/new/dlt-hub/dlt-llm-code-playground?ref=create-pipeline)

## DHelp

## Ask a question

Welcome to "Codex Central", your next-gen help center, driven by OpenAI's GPT-4 model. It's more than just a forum or a FAQ hub – it's a dynamic knowledge base where coders can find AI-assisted solutions to their pressing problems. With GPT-4's powerful comprehension and predictive abilities, Codex Central provides instantaneous issue resolution, insightful debugging, and personalized guidance. Get your code running smoothly with the unparalleled support at Codex Central - coding help reimagined with AI prowess.