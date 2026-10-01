# Python tokenizer (GPT-2)

Load GPT-2's tokenizer with Hugging Face `transformers` and use it to turn text
into token IDs, view the sub-word pieces, and decode the IDs back into text.

## Run it

```shell
make build   # clean, create a fresh .venv and install requirements
make run     # run the POC
make test    # run the tests
```

The first run downloads the GPT-2 tokenizer files (~2 MB) from the Hugging Face
Hub into `~/.cache/huggingface`. PyTorch isn't needed, because this POC only
uses the tokenizer and never loads the model. `main.py` sets
`TRANSFORMERS_NO_ADVISORY_WARNINGS=1` to hide the "PyTorch was not found" notice.

Expected output:

```
Token IDs: [20185, 6554, 1429, 16326, 18306, 0]
Tokens: ['AI', 'Ġagents', 'Ġprocess', 'Ġtokens', 'Ġefficiently', '!']
Decoded: AI agents process tokens efficiently!
```

`Ġ` is how GPT-2's byte-level BPE shows a leading space. The space is part of
the token, so ` agents` and `agents` have different IDs.

## Things to try

- Swap `"gpt2"` for `"bert-base-uncased"` and compare the pieces (`##` marks
  word continuations, and the text gets `[CLS]`/`[SEP]` added).
- Tokenize a long or rare word (e.g. `"antidisestablishmentarianism"`) and see
  how it breaks into sub-words.
- Try non-English text or emoji and count how many tokens they use.
