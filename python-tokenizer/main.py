import os

# Only the tokenizer is used, so silence the "PyTorch was not found" advisory.
# Must be set before transformers is imported.
os.environ.setdefault("TRANSFORMERS_NO_ADVISORY_WARNINGS", "1")

from transformers import AutoTokenizer  # noqa: E402


def main():
    # Load a tokenizer associated with a model family
    tokenizer = AutoTokenizer.from_pretrained("gpt2")

    text = "AI agents process tokens efficiently!"

    # Convert text to integer token IDs
    token_ids = tokenizer.encode(text)
    print("Token IDs:", token_ids)

    # View the actual sub-word string chunks
    tokens = tokenizer.tokenize(text)
    print("Tokens:", tokens)

    # Reconstruct the original text from IDs
    decoded_text = tokenizer.decode(token_ids)
    print("Decoded:", decoded_text)


if __name__ == "__main__":
    main()
