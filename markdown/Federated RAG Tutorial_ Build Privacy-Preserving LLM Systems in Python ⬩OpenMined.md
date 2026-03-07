---
title: "Federated RAG Tutorial: Build Privacy-Preserving LLM Systems in Python ⬩OpenMined"
source: "https://openmined.org/blog/tutorial-turn-any-llm-into-an-expert-assistant-with-federated-rag-part-1/"
author:
  - "[[Siddhant Rai]]"
  - "[[Irina Bejan]]"
published: 2025-10-24
created: 2025-12-16
description: "Build Federated RAG from scratch in Python. Learn how LLMs can access distributed private data sources without compromising privacy. Code included."
tags:
  - "clippings"
---
- 2 months ago

**TL;DR:** LLMs often fail on domain-specific questions, not from lack of capability, but from missing access to expert data. RAG extends their reach with external context, but only for data one has access to, while much of it is locked behind privacy and IP walls. In this tutorial, we build Federated RAG from scratch and run it across a live network of data, showing how models can tap into privately held knowledge.

Let’s get started, we’re 50 lines of Python from turning an LLM into a domain expert.

**Just Give Me The Code**:

```python
from syft_hub import Client

cl = Client()

# Choose a data source
hacker_news_source = cl.load_service("demo@openmined.org/hackernews-top-stories")
arxiv_source = cl.load_service("demo@openmined.org/arvix-articles")
github_source = cl.load_service("demo@openmined.org/github-trending")

# Choose an LLM to combine insights
claude_llm = cl.load_service("aggregator@openmined.org/claude-3.5-sonnet")

# Run FedRAG insights
fedrag_pipeline = cl.pipeline(
    data_sources=[hacker_news_source, arxiv_source, github_source],
    synthesizer=claude_llm
)

query = "What methods can help improve context in LLM agents?"
result = fedrag_pipeline.run(messages=[{"role": "user", "content": query}])

print(result)
```

This post is part of a series on **how AI can unlock much more knowledge** by tapping into data owned by others, in a federated way, without ever seeing or exposing it.

*Sign up here to get new learning resources sent to your inbox as they are published:*

⬩⬩⬩

## Intro: Closing the Expert Data Gap with Federated RAG

*If you have already heard of RAG (Retrieval-Augmented Generation), but would still like a quick recap, [check out our primer on RAG](https://openmined.org/?p=7502).*

LLMs perform well on open-domain questions (*i.e. “Who wrote Romeo and Juliet?”*) since they are trained on broad public datasets. However, they struggle on domain-specific questions: a doctor asking about drug interactions, a lawyer checking precedents, or a patient comparing insurance claims will usually get vague or wrong answers. The issue isn’t model size or architecture: it’s that the data they need simply isn’t part of the training set.

That data lives elsewhere: hospital records, law firm documents, insurance databases, personal files or corporate secrets. Moreover, it stays siloed because current AI approaches all share the same flaw: data owners must hand over their raw data and lose control, which introduces legal, privacy and intellectual property risks. Understandably, most organizations say no.

![](https://openmined.org/wp-content/uploads/2025/10/OpenMined-Live-Graphics-2.svg)

The result is akin to narrow listening: LLMs fall back on their training set or, with RAG, rely on the limited snippets that fit into their context window. But real-world problems quickly overwhelm this setup:

- Medication: Is drug X interfering with my diabetes treatment? → requires pharma records, clinical trials, and clinician notes.
- Legal precedents: Which rulings apply to a non-compete lawsuit? → requires case law across jurisdictions.
- Immigration: How long do EB-2 visas take at different centers? → requires outcomes from thousands of past applicants.

In each case, the information exists – just not somewhere you can easily access. It’s often owned by others, protected for privacy, locked behind systems, or scattered across silos.

The alternative is **[broad listening](https://openmined.org/blog/what-is-broad-listening/)**. Instead of guessing when snippets are insufficient, the model can “walk the halls” and consult multiple sources directly — like a student talking to several teachers instead of relying on one textbook. The result is far more valuable: a trustworthy expert assistant, tailored to your needs.

Of course, broad listening only works if everyone gets to set their own boundaries. It’s not about pooling everyone’s data into one big bucket, but about coordination without centralization. Each participant keeps control: deciding what to share, what stays private, and under what conditions their data can be queried.

This is the essence of federated RAG. In the next section, we will start with building it from scratch.

⬩⬩⬩

## Step 1: Build Federated RAG

*What you’ll learn:*

- Federate RAG Building Blocks – local indexing, secure retrieval, and result aggregation.
- Putting it together – synthesizing answers with an LLM
- Querying real, federated data network via Syft Hub

A **Federated RAG (FedRAG)** system is a distributed form of RAG. Instead of centralizing data in one repository, a query is broadcasted to participating peers. Each peer searches its own local corpus, retrieves the most relevant snippets, and returns them to the orchestrating model. The model then integrates these distributed results into a single, grounded response.

In effect, FedRAG operationalizes broad listening at a network level: models can reason across decentralized data sources while preserving data ownership, privacy, and domain autonomy.

That said, let us start with deep-dive of our setup.

```python
# ======================================================
# ⚡ Compact Federated RAG
# ======================================================

!pip install llama-index-core llama-index-embeddings-huggingface

from llama_index.embeddings.huggingface import HuggingFaceEmbedding
from llama_index.core.settings import Settings
from scipy.spatial import distance

# ---- Embedding Model ----
def create_embedding_model(model_name="BAAI/bge-small-en-v1.5"):
    model = HuggingFaceEmbedding(model_name=model_name)
    Settings.embed_model = model
    return model

def create_embedding(model, text):
    return model.get_query_embedding(text)

# ---- Local Index ----
def build_local_index(name, docs, model):
    index = {doc_id: create_embedding(model, text) for doc_id, text in docs.items()}
    return name, docs, index

# ---- Local + Federated Retrieval ----
def local_retrieve(index, query, model, k=1):
    q_vec = create_embedding(model, query)
    ranked = sorted(index.items(), key=lambda x: distance.cosine(x[1], q_vec))
    return [(doc_id, distance.cosine(embed, q_vec)) for doc_id, embed in ranked[:k]]

def federated_query(nodes, query, model, per_node_k=1, final_k=1):
    candidates = [(dist, name, doc_id)
        for name, docs, index in nodes
        for doc_id, dist in local_retrieve(index, query, model, per_node_k)]
    winners = sorted(candidates, key=lambda x: x[0])[:final_k]
    return [docs[doc_id] for _, name, doc_id in winners for n, docs, _ in nodes if n == name]

# ---- Context Builder + Mock LLM ----
def build_context(query, contexts):
    ctx = "\n\n".join(contexts)
    return f"Context:\n{ctx}\n\nQuestion: {query}\nAnswer concisely using only the context."

def run_llm(prompt):
    return f"[Mock LLM Response] {prompt[:200]}..."

# ---- Run Demo (Single Query) ----
if __name__ == "__main__":
    model = create_embedding_model()
    shared_docs = {
        "doc1": "Artificial intelligence is transforming healthcare by improving diagnostics.",
        "doc2": "Quantum computing leverages superposition and entanglement for computation.",
        "doc3": "Federated learning enables collaboration without sharing raw data."
    }
    nodeA = build_local_index("Hospital_A", shared_docs, model)
    nodeB = build_local_index("Hospital_B", shared_docs, model)
    nodes = [nodeA, nodeB]

    query = "How is AI used?"
    contexts = federated_query(nodes, query, model)
    prompt = build_context(query, contexts)
    answer = run_llm(prompt)

    print(f"\nQ: {query}")
    print(f"→ Retrieved: {contexts[0]}")
    print(f"→ Answer:\n{answer}")
```

### 1.1: Load Embedding Model

First, let’s load a small, efficient embedding model to turn text into vectors for query–document similarity.

```python
# ======================================================
# 🧩 Step 1.1: Load an Embedding Model
# ======================================================

from llama_index.embeddings.huggingface import HuggingFaceEmbedding
from llama_index.core.settings import Settings

def create_embedding_model(model_name="BAAI/bge-small-en-v1.5"):
    """Initializes and returns the HuggingFaceEmbedding model."""
    embedding_model = HuggingFaceEmbedding(model_name=model_name)
    Settings.embed_model = embedding_model           # make it globally available in LlamaIndex
    return embedding_model

def create_embedding(embedding_model, data):
    """Creates an embedding for the given data using the provided model."""
    return embedding_model.get_query_embedding(data)

# ---- Usage Example ----
# Initialize the embedding model
embedding_model = create_embedding_model()

# Example text to embed
data_to_embed = "Artificial intelligence is transforming healthcare."
vector = create_embedding(embedding_model, data_to_embed)

# Display results
print("\nInput:", data_to_embed)
print("Output length:", len(vector))
print("First 5 values:", [round(v, 4) for v in vector[:5]])
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-06-at-03.20.05.mov"></video>
- Loads the BAAI/bge-small-en-v1.5 sentence embedding model
- `create_embedding()` method takes care of text to vector conversion
- Stores it inside `Settings.embed_model`, so that any LlamaIndex component knows which model to use.

### 1.2: Create Local Index

Imagine a hospital, a law office, or just a friend with private notes. Each keeps their own *local* index.

```python
# ======================================================
# 🧩 Step 1.2: Build a Local Index
# ======================================================

def build_local_index(name, private_docs, embedding_model):
    """Builds a simple local index mapping each doc_id into an embedding vector."""
    local_index = {
        doc_id: create_embedding(embedding_model, text)
        for doc_id, text in private_docs.items()
    }
    return name, private_docs, local_index

# ---- Usage Example ----
private_docs = {
    "doc1": "Artificial intelligence is transforming healthcare by improving diagnostics.",
    "doc2": "Quantum computing leverages superposition and entanglement for computation.",
    "doc3": "Federated learning enables collaboration without sharing raw data."
}

node_name, node_private_docs, node_local_index = build_local_index(
    name="Hospital_A",
    private_docs=private_docs,
    embedding_model=embedding_model,  # from Step 1.1
)

print(f"\nFederated Node: {node_name}")
print("Private docs:", list(node_private_docs.keys()))
print("Sample embedding dimension:", len(node_local_index['doc1']))
print("First 5 values of doc1 embedding:",
      [round(v, 4) for v in node_local_index['doc1'][:5]])
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-06-at-03.27.33.mov"></video>
- Here, each peer (hospital, company, friend, etc.) is represented by a `FederatedNode` (replicated by build\_local\_index).
- Each node has:
	- `private_docs`: raw text documents it owns.
	- `index`: a dictionary mapping each `doc_id` → its vector embedding.
	- When you call `build_local_index()`, the text is embedded and stored locally.
![](https://openmined.org/wp-content/uploads/2025/10/OpenMined-Live-Graphics-5.svg)

*This runs on each peer’s machine in a distributed network (Distributed Indexing), so that the index and documents remain private to each peer. Nothing is shared.*

### 1.3: Local Retrieval

A peer can now answer questions only from its own knowledge.

```python
# ======================================================
# 🧩 Step 1.3: Local Retrieval
# ======================================================

from scipy.spatial import distance

def local_retrieve(index, query, embedding_model, k: int = 1):
    """Retrieve top-k most similar documents from a node’s local index."""
    q_vec = create_embedding(embedding_model, query)
    ranked = sorted(index.items(), key=lambda x: distance.cosine(x[1], q_vec))
    return [(doc_id, distance.cosine(embed, q_vec)) for doc_id, embed in ranked[:k]]

# ---- Usage Example ----
query = "How is AI used in hospitals?"
results = local_retrieve(
    index=node_local_index,          # from Step 1.2
    query=query,
    embedding_model=embedding_model, # from Step 1.1
    k=2
)

print(f"\nQuery: {query}")
print("Top retrieved docs with cosine distance:")
for doc_id, score in results:
    print(f" - {doc_id}: {score:.4f}")
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-06-at-03.31.26.mov"></video>
- The query text is embedded into a vector (`q_vec`).
- The query vector is compared against the embeddings in the node’s index using `distance.cosine`, returning the top-k most relevant documents to the query and their similarity scores.

This is a local retrieval step, so each each node only searches *its own private index*. No raw data leaves the node until we run a federated query (next step).

### 1.4: Federated Retrieval

Now imagine multiple peers: each keeps their data private, but can return relevant snippets.

![](https://openmined.org/wp-content/uploads/2025/10/OpenMined-Live-Graphics-6.svg)

Then if someone submits a question (e.g., *“Who in my friends group knows LLM agents?”*); we would like to use our FedRAG to respond.

```python
# ======================================================
# 🧩 Step 1.4: Federated Retrieval
# ======================================================

def federated_query(nodes_data, query, embedding_model, per_node_k: int = 1, final_k: int = 1):
    """Perform federated retrieval across multiple peers without sharing raw data."""
    candidates = []  # (distance, node_name, doc_id)
    for node_name, private_docs, local_index in nodes_data:
        for doc_id, dist in local_retrieve(local_index, query, embedding_model, k=per_node_k):
            candidates.append((dist, node_name, doc_id))

    # Sort by cosine distance (smaller = more similar)
    candidates.sort(key=lambda x: x[0])
    winners = candidates[:final_k]

    # Retrieve actual text from the winning nodes
    contexts = [
        private_docs[doc_id]
        for _, node_name, doc_id in winners
        for name, private_docs, _ in nodes_data
        if name == node_name and doc_id in private_docs
    ]
    return contexts

# ---- Usage Example ----
shared_private_docs = {
    "doc1": "Artificial intelligence is transforming healthcare by improving diagnostics.",
    "doc2": "Quantum computing leverages superposition and entanglement for computation.",
    "doc3": "Federated learning enables collaboration without sharing raw data."
}

node1_data = ("Hospital_A", shared_private_docs,
              build_local_index("Hospital_A", shared_private_docs, embedding_model)[2])
node2_data = ("Hospital_B", shared_private_docs,
              build_local_index("Hospital_B", shared_private_docs, embedding_model)[2])

nodes_list = [node1_data, node2_data]

for q in ["How is AI used?", "What is quantum computing?", "How is federated learning used?"]:
    contexts = federated_query(nodes_list, q, embedding_model, per_node_k=1, final_k=1)
    print(f"\nQ: {q}\n→ Retrieved Contexts: {contexts}")
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-06-at-03.35.12.mov"></video>

Now you’ll see Node2 returns the right snippet, without Node1 or Node3 ever sharing their private data. This is a bit hard to visualize here, as everything runs on on one machine; however, this setup behaves the same way when running over a distributed network (which we will see in the upcoming section!).

### 1.5: Generate Answer

Finally, we can pass the retrieved nodes/snippets into an LLM (Claude, ChatGPT, Gemini, etc.) for generation.

```python
# ======================================================
# 🧩 Step 1.5: Generate Final Answer (LLM + Context Builder)
# ======================================================

# ---- Context Construction ----
def build_context(query, contexts):
    """Merge retrieved contexts with the user query"""
    joined_contexts = "\n\n".join(contexts)
    prompt = (
        "You are an expert assistant helping answer questions based on private documents.\n\n"
        f"Context:\n{joined_contexts}\n\n"
        f"Question: {query}\n\n"
        "Answer clearly and concisely using only the information from the context above."
    )
    return prompt

# ---- LLM Wrapper (model-agnostic) ----
def run_llm(prompt, use_openai=False):
    """Mock text generation — you can replace it later"""
    if use_openai:
        # from llama_index.llms.openai import OpenAI
        # llm = OpenAI(model="gpt-4o-mini")
        # response = llm.complete(prompt)
        # return response.text
        pass

    # Default mock response
    return f"[Mock LLM Response] {prompt[:200]}..."

# ---- Usage Example ----
query = "How is AI used?"
# 1️⃣ Retrieve context from federated nodes
contexts = federated_query(nodes_list, q, embedding_model, per_node_k=1, final_k=1)

# 2️⃣ Build context-aware prompt
prompt = build_context(q, contexts)

# 3️⃣ Run LLM (mock by default)
answer = run_llm(prompt, use_openai=False)

print(f"\nQ: {query}")
print(f"\nPrompt Sent to LLM:\n{'-'*60}\n{prompt}\n{'-'*60}")
print(f"\nGenerated Answer:\n{answer}")
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-06-at-03.50.00.mov"></video>

⬩⬩⬩

## Step 2: Query a Federated Network

So far, we’ve only simulated nodes on *our laptop*. Now, let’s connect to a **real network** where illustrative peers already exist (e.g. Hacker News, arXiv, GitHub).

This is where things get fun — you’ll be able to query live sources in just a few lines of code.

### 2.1: Connect to Syft Hub

```python
%pip install syfthub[installer]
```
```python
from syft_hub import Client
cl = Client()
cl
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-24-at-22.16.01.mov"></video>

*What are we setting up now?*

- [**SyftBox** i](https://syftbox.net/) s a protocol to make data sharing easy: it’s like DropBox, but networked. This way, any machine can become a data peer without opening complicated firewalls or open ports.
- [**syft-hub**](https://syft-protocol.openmined.org/syft-sdk/) is a python SDK that allows one to discover and interact with the data sources and models running on Syft

### 2.2: Load Data Sources

Think of these as **federated peers** that own their data, but you can access it through the retrieval mechanism. As a result, you never see all their data – but only what you might explicitly request and are allowed to retrieve.

```python
# Load some demo data sources
hacker_news_source = cl.load_service("demo@openmined.org/hackernews-top-stories")
arxiv_source = cl.load_service("demo@openmined.org/arvix-agents")
github_source = cl.load_service("demo@openmined.org/github-trending")

print("Data sources ready ✅")
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/source_loading.mov"></video>
- Create a client that allows you to interact with syft and the synced folder
- Each `load_service(...)` returns a handle that allows you to query that source at run time (no raw data is pulled locally).
- These are indeed illustrative, public sources. As the network grows and more non-public sources join, you’ll use them just the same way!

### 2.3: Load an LLM

We need one LLM model that can take insights from all peers and stitch them into a final answer – here, we are using Claude. There’s more models linked to the network that you can try *– note that when using the network for the first time, you get $20 worth of credits to explore freely anything.*

```python
claude_llm = cl.load("aggregator@openmined.org/claude-3.5-sonnet")
print("Aggregator LLM loaded ✅")
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/claude_loading.mov"></video>

### 2.4: Run Federated RAG

A pipeline defines:

- which peers (data sources) will be queried,
- which LLM will synthesize the answer.
```python
pipeline = cl.pipeline(
    data_sources=[hacker_news_source, arxiv_source, github_source],
    synthesizer=[claude_llm]
)
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/pipeline-definition.mov"></video>
```python
query = "What methods can help improve context in LLM agents?"

result = fedrag_pipeline.run(messages=[{"role": "user", "content": query}])

print("Q:", query)
print("A:", result)
```
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-06-at-00.34.27.mov"></video>

Once a query is run, each source computes on their data the local retrieval operation *(just like in Section 2!)* and returns top matches. The pipeline, acting as the `FederatedCoordinator`, will concatenate the sources into the model context and then call the model (the LLM we chose).

What you get back here? A single, LLM-generated answer that gets the best out of HackerNews’s discussions, arXiv papers on agents and trending Github repos.

⬩⬩⬩

## Where You Can Tinker

- **Try follow-up queries** using `pipeline.run()`; [more here](https://syft-protocol.openmined.org/syft-sdk/api-pipelines.html)
- **Explore more sources**: run `cl.show_services()` to see what other data services or models are available and try a new pipeline! [more here](https://syft-protocol.openmined.org/syft-sdk/api-client.html#service-discovery)
<video controls="controls" src="https://openmined.org/wp-content/uploads/2025/10/Screen-Recording-2025-10-05-at-23.43.39.mov"></video>
- ****Discover SyftBox****: [Install](https://syft-protocol.openmined.org/syft-router/index.html) it, setup your first data source and start querying your own files!
- **Invite a friend** **to query your file**: after setting up your data source and added a test file, ask a friend to send you a query!

In the upcoming parts of this series, we’ll dive deeper into how you can define your own AI service effectively. If you encounter issues, [join our Slack and don’t hesitate to drop us a message!](https://openmined.org/slack/)

⬩⬩⬩

## From Private Data to Federated Intelligence

Federated RAG isn’t just another architecture pattern: it’s a shift in how intelligence itself is built. Instead of copying data into central silos, models learn to travel to the data – negotiating access, running local queries, and combining insights without ever seeing the raw source.

But as soon as models start reaching into private data sources, new questions pop up fast (and we bet you’ve already thought of a few!): how to send queries safely, keep responses consistent without a global schema, maintain speed and privacy, and design the right incentives for this approach to thrive.

The demo gives a glimpse of what’s ahead: AI that learns *from* data without ever having to *own* it.  
In the next parts of this series, we’ll unpack these challenges one by one – across **machine learning, engineering, privacy, and distribution** – and show how various technologies come together to make federated AI real. Stick around if you’re curious for more depth.

![](https://openmined.org/wp-content/uploads/2025/10/OpenMined-Live-Graphics-9.svg)

⬩⬩⬩

## Conclusion

LLMs often miss on hard, domain-specific questions not because they lack intelligence, but because the expertise they need is out of reach. RAG helps, but only with the data you already have – and that’s inherently limited. Most valuable data remains inaccessible.

To address this, we built **Federated RAG** from the ground up: showing how a single peer can index and retrieve locally, and how models can query across a live, federated network of data using Syft.

The outcome is the best of both worlds:

- The scale and fluency of frontier LLMs.
- The depth and trustworthiness of domain expertise.

If you want Claude to stop guessing and start reasoning like a true expert, the path forward is clear: tap into data where it lives with **Federated RAG**.

---

---

*If you’re curious how **Federated RAG** can tackle real-world challenges like privacy, trust, scaling, data monetisation or integrating it into your apps and workflows, make sure to sign up for the series*:

*Till then happy Learning. Bye 👋*

**Resources**

- [SyftBox](https://www.syftbox.net/)
- [syft-hub](https://syft-protocol.openmined.org/syft-sdk/)
- [Syft Router](https://syft-protocol.openmined.org/syft-router/index.html)
- [Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks](https://arxiv.org/abs/2005.11401) (Lewis et al., 2020) —  *The original RAG paper from Facebook AI*
- [Introduction to Retrieval Augmented Generation (RAG)](https://huggingface.co/blog/rag) (Hugging Face Blog) — *A beginner-friendly walkthrough with examples*

###### Continued Reading...

[View all posts](https://openmined.org/blog/)

![](https://openmined.org/wp-content/uploads/2025/03/OpenMined-Icon-large.svg)

**OpenMined** is a 501(c)(3) non-profit foundation and a global community on a mission to create the public network for non-public information.

With your support, we can unlock the world’s insights while making privacy accessible to everyone.

We can do it, with your help.

- [About the foundation](https://openmined.org/foundation/)
- [Get involved](https://openmined.org/get-involved/)

Secure Donation

Philanthropist looking for more?

[Contact us](https://openmined.org/major-gift/)