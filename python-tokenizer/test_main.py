# Runs the real GPT-2 tokenizer (downloaded from the Hugging Face Hub on first
# use) so the test fails if the POC's output stops matching what GPT-2 produces.

import main


def test_main_prints_gpt2_ids_tokens_and_roundtrips_the_text(capsys):
    main.main()
    out = capsys.readouterr().out

    assert "Token IDs: [20185, 6554, 1429, 16326, 18306, 0]" in out
    # "Ġ" is how GPT-2's byte-level BPE marks a leading space.
    assert "Tokens: ['AI', 'Ġagents', 'Ġprocess', 'Ġtokens', 'Ġefficiently', '!']" in out
    assert "Decoded: AI agents process tokens efficiently!" in out
