---
title: "Structured Outputs Create False Confidence"
source: "https://boundaryml.com/blog/structured-outputs-create-false-confidence"
author:
  - "[[Sam Lijin]]"
published: December 14
created: 2025-12-16
description: "Constrained decoding seems like the greatest thing since sliced bread, but it forces models to prioritize output conformance over output quality."
tags:
  - "clippings"
---
Engineering about 23 hours ago 7 min read

Constrained decoding seems like the greatest thing since sliced bread, but it forces models to prioritize output conformance over output quality.

![Sam Lijin](https://boundaryml.com/_next/image?url=%2Fprofile-sam.png&w=1080&q=75)

Sam Lijin

If you use LLMs, you've probably heard about structured outputs. You might think they're the greatest thing since sliced bread. Unfortunately, **structured outputs also degrade response quality**.

Specifically, if you use an LLM provider's structured outputs API, you will get a lower quality response than if you use their normal text output API:

- ⚠️ you're more likely to make mistakes when extracting data, even in simple cases;
- ⚠️ you're probably not modeling errors correctly;
- ⚠️ it's harder to use techniques like chain-of-thought reasoning; and
- ⚠️ in the extreme case, it can be easier to steal your customer data using prompt injection.

These are very contentious claims, so let's start with an example: extracting data from a receipt.

![Receipt with fractional quantities](https://boundaryml.com/receipt-fractional-quantity.jpg)

If I use an LLM to extract the receipt entries, it should be able to tell me that one of the items is `(name="banana", quantity=0.46)`, right?

Well, using OpenAI's structured outputs API with `gpt-5.2` - released literally this week! - it will claim that the banana quantity is `1.0`:

```
{
  "establishment_name": "PC Market of Choice",
  "date": "2007-01-20",
  "total": 0.32,
  "currency": "USD",
  "items": [
    {
      "name": "Bananas",
      "price": 0.32,
      "quantity": 1
    }
  ]
}
```

However, with the *same model*, if you just use the completions API and then parse the output, it will return the correct quantity:

```
{
  "establishment_name": "PC Market of Choice",
  "date": "2007-01-20",
  "total": 0.32,
  "currency": "USD",
  "items": [
    {
      "name": "Bananas",
      "price": 0.69,
      "quantity": 0.46
    }
  ]
}
```
Click here to see the code that was used to generate the above outputs.

This code is also [available on GitHub](https://gist.github.com/sxlijin/867b812ceb1aa97872937bebe5cfb4be).

```
#!/usr/bin/env -S uv run

# /// script

# requires-python = ">=3.10"

# dependencies = ["openai", "pydantic", "rich"]

# ///

"""

If you have uv, you can run this code by saving it as structured_outputs_quality_demo.py and then running:

  chmod u+x structured_outputs_quality_demo.py

  ./structured_outputs_quality_demo.py

This script is a companion to https://boundaryml.com/blog/structured-outputs-create-false-confidence

"""

import json

import re

from openai import OpenAI

from pydantic import BaseModel, Field

from rich.console import Console

from rich.pretty import Pretty

class Item(BaseModel):

    name: str

    price: float = Field(description="per-unit item price")

    quantity: float = Field(default=1, description="If not specified, assume 1")

class Receipt(BaseModel):

    establishment_name: str

    date: str = Field(description="YYYY-MM-DD")

    total: float = Field(description="The total amount of the receipt")

    currency: str = Field(description="The currency used for everything on the receipt")

    items: list[Item] = Field(description="The items on the receipt")

client = OpenAI()

console = Console()

def run_receipt_extraction_structured(image_url: str):

    """Call the LLM to extract receipt data from an image URL and return the raw response."""

    prompt_text = (

        """

Extract data from the receipt.

"""

    )

    response = client.beta.chat.completions.parse(

        model="gpt-5.2-2025-12-11",

        messages=[

            {

                "role": "system",

                "content": "You are a precise receipt extraction engine. Return only structured data matching the Receipt schema.",

            },

            {

                "role": "user",

                "content": [

                    {

                        "type": "text",

                        "text": prompt_text,

                    },

                    {"type": "image_url", "image_url": {"url": image_url}},

                ],

            },

        ],

        response_format=Receipt,

    )

    return response.choices[0].message.content, response.choices[0].message.parsed

def run_receipt_extraction_freeform(image_url: str):

    """Call the LLM to extract receipt data from an image URL and return the raw response."""

    prompt_text = (

        """

Extract data from the receipt.

Explain your reasoning, then answer in JSON:

{

  establishment_name: string,

  // YYYY-MM-DD

  date: string,

  // The total amount of the receipt

  total: float,

  // The currency used for everything on the receipt

  currency: string,

  // The items on the receipt

  items: [

    {

      name: string,

      // per-unit item price

      price: float,

      // If not specified, assume 1

      quantity: float,

    }

  ],

}

"""

    )

    response = client.beta.chat.completions.parse(

        model="gpt-5.2-2025-12-11",

        messages=[

            {

                "role": "user",

                "content": [

                    {

                        "type": "text",

                        "text": prompt_text,

                    },

                    {"type": "image_url", "image_url": {"url": image_url}},

                ],

            },

        ],

    )

    return response.choices[0].message.content, json.loads(re.search(r"\`\`\`json(.*?)\`\`\`", response.choices[0].message.content, flags=re.DOTALL).group(1))

def main() -> None:

    images = [

        {

            "title": "Parsing receipt: fractional quantity",

            "url": "https://boundaryml.com/receipt-fractional-quantity.jpg",

            "expected": "You should expect quantity to be 0.46."

        },

        {

            "title": "Parsing receipt: elephant",

            "url": "https://boundaryml.com/receipt-elephant.jpg",

            "expected": "You should expect an error."

        },

        {

            "title": "Parsing receipt: currency exchange",

            "url": "https://boundaryml.com/receipt-currency-exchange.jpg",

            "expected": "You should expect a warning about mixed currencies."

        },

    ]

    print("This is a demonstration of how structured outputs create false confidence.")

    for entry in images:

        title = entry["title"]

        url = entry["url"]

        completion_structured_content, _ = run_receipt_extraction_structured(url)

        completion_freeform_content, _ = run_receipt_extraction_freeform(url)

        console.print("[cyan]--------------------------------[/cyan]")

        console.print(f"[cyan]{title}[/cyan]")

        console.print(f"Asking LLM to parse receipt from {url}")

        console.print(entry['expected'])

        console.print()

        console.print("[cyan]Using structured outputs:[/cyan]")

        console.print(completion_structured_content)

        console.print()

        console.print("[cyan]Parsing free-form output:[/cyan]")

        console.print(completion_freeform_content)

if __name__ == "__main__":

    main()
```

Now, what happens if someone submits a picture of an elephant?

Or a currency exchange receipt?

![currency exchange receipt](https://boundaryml.com/receipt-currency-exchange.jpg)

In these scenarios, you want to let the LLM respond using text. You want it to be able to say that, hey, you're asking me to parse a receipt, but you gave me a picture of an elephant, I can't parse an elephant into a receipt.

If you force the LLM to respond using structured outputs, you take that ability away from the LLM. Sure, you'll get an object that satisfies your output format, but it'll be meaningless. It's like when you file a bug report, and the form has 5 mandatory fields about things that have nothing to do with your bug, but you have to put *something* in those fields to file the bug report: the stuff you put in those fields will probably be useless.

## I can design my output format better!

Yes and no.

Yes, you can tell your LLM to return `{ receipt data } or { error }`. But what kinds of errors are you going to ask it to consider?

- What kind of error should it return if there's no `total` listed on the receipt? Should it even return an error or is it OK for it to return `total = null`?
- What if it can successfully parse 7 of 8 items on the receipt, but it's not sure about the 8th item? Should it return (1) the 7 successfully parsed items and a partial parse of the 8th item, (2) only the 7 successfully parsed items and discard the 8th or (3) fail parsing entirely?
- What if someone submits a picture of an elephant? What kind of error should be returned in that case?

In addition, as you start enumerating all of these errors, you run into the [pink elephant problem](https://arxiv.org/abs/2402.07896): the more your prompt talks about errors, the more likely the LLM is to respond with an error.

Think of it this way: if someone presses Ctrl-C when running your binary, it is a Good Thing that the error can propagate all the way up through your binary, without you having to explicitly write `try { ... } catch CtrlCError { ... }` in every function in your codebase.

In the same way that you often want to allow errors to just propagate up while writing software, and only explicitly handle *some* errors, your LLM should be allowed to respond with errors in whatever fashion it wants to.

## Chain-of-thought is crippled by structured outputs

"Explain your reasoning step by step" is a magic incantation that seemingly makes LLMs much smarter. It also turns out that this trick doesn't work nearly as well when using structured outputs, and [we've known this since Aug 2024](https://arxiv.org/abs/2408.02442).

To understand this finding, the intuition I like to use, is to think of every model of having an intelligence "budget", and that if you try to force an LLM to reason in a very specific format, you're making the LLM spend intelligence points on useless work.

To make this more concrete, let's use another example. If you prompt an LLM to give you JSON output and reason about it step-by-step, its response will look something like this:

```
If we think step by step we can see that:

1. The email is from Amazon, confirming the status of a specific order.
2. The subject line says "Your Amazon.com order of 'Wood Dowel Rods...' has shipped!" which indicates that the order status is 'SHIPPED'.
3. [...]

Combining all these points, the output JSON is:

\`\`\`json
{
     "order_status": "SHIPPED",
     [...]
}
\`\`\`
```

Notice that although the response contains valid JSON, the response itself is not valid JSON, because of the reasoning text at the start. In other words, you can't use basic chain-of-thought reasoning with structured outputs.

You *could* modify your schema, and add `reasoning: string` fields to your output schema, and let the LLM respond with something like this:

```
{
  "reasoning": "If we think step by step we can see that:\n\n 1. The email is from Amazon, confirming the status of a specific order.\n2. The subject line says \"Your Amazon.com order of 'Wood Dowel Rods...' has shipped!\" [...]
  ...
}
```

In other words, if you're using a `reasoning` field with structured outputs, instead of simply asking the LLM to reason about its answer, you're also forcing it to escape newlines and quotes and format that correctly as JSON. You're basically asking the LLM to [put a cover page on its TPS report](https://www.youtube.com/watch?v=jsLUidiYm0w&t=19s).

## Why are structured outputs bad?

(To understand this section, you'll need a bit of background on [transformer models](https://www.3blue1brown.com/lessons/gpt#what-exactly-is-a-transformer), specifically how [logit sampling](https://github.com/karpathy/nanoGPT/blob/3adf61e154c3fe3fca428ad6bc3818b27a3b8291/model.py#L323-L328) works. Feel free to skip this section if you don't have this background.)

Model providers like OpenAI and Anthropic implement structured outputs using a technique called [constrained decoding](https://openai.com/index/introducing-structured-outputs-in-the-api/#constrained-decoding):

> By default, when models are sampled to produce outputs, they are entirely unconstrained and can select any token from the vocabulary as the next output. This flexibility is what allows models to make mistakes; for example, they are generally free to sample a curly brace token at any time, even when that would not produce valid JSON. In order to force valid outputs, we constrain our models to only tokens that would be valid according to the supplied schema, rather than all available tokens.

In other words, constrained decoding applies a filter during sampling that says, OK, given the output that you've produced so far, you're only allowed to consider certain tokens.

For example, if the LLM has so far produced `{"quantity": 51`, and you're constraining output decoding to satisfy `{ quantity: int, ... }`:

- `{"quantity": 51.2` would not satisfy the constraint, so `.2` is not allowed to be the next token,
- `{"quantity": 51,` would satisfy the constraint, so `,` is allowed to be the next token,
- `{"quantity": 510` would satisfy the constraint, so `0` is allowed to be the next token (albeit, in this example, with low probability!),

But if the LLM actually wants to answer with `51.2` instead of `51`, it isn't allowed to, because of our constraint!

Sure, if you're using constrained decoding to force it to return `{"quantity": 51.2}` instead of `{"quantity": 51.2,}` - because trailing commas are not allowed in JSON - it'll probably do the right thing. But that's something you can write code to handle, which leads me to my final point.

## Just parse the output

OK, so if structured outputs are bad, then what's the solution?

It turns out to be really simple: let the LLM do what it's trained to do. Allow it to respond in a free-form style:

- let it [refuse to count](https://chatgpt.com/share/691ac9b7-47a0-800a-a9a7-c0302f463168) the number of entries in a list
- let it [warn you](https://chatgpt.com/share/693f1edb-1c54-800a-8b4b-db146f856b0c) when you've given it contradictory information
- let it [tell you the correct approach](https://chatgpt.com/share/693f1cd0-6a20-800a-aca9-09599216badf) when you inadvertently ask it to use the wrong approach

Using structured outputs, via constrained decoding, makes it much harder for the LLM to do any of this. Even though you've crafted a guarantee that the LLM will return a response in exactly your requested output format, that guarantee comes at the cost of the *quality* of that response, because you're forcing the LLM to prioritize complying with your output format over returning a high-quality response. That's why structured outputs create false confidence: it's entirely non-obvious that you're sacrificing output quality to achieve output conformance.

Parsing the LLM's free-form output, by contrast, enables you to retain that output quality.

(In a scenario where an attacker is trying to convince your agent to do something you didn't design it to do, the parsing also serves as an effective defense-in-depth layer against malicious prompt injection.)

This is [why BAML - our open-source, local-only DSL - uses schema-aligned parsing](https://docs.boundaryml.com/guide/introduction/why-baml#3-schema-aligned-parsing-sap): we believe letting the LLM respond in as natural a fashion as possible is the most effective way to get the highest quality response from it.