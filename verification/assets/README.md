# Comparison font

Current Latin parity font: `FoliaParityLatin-Bold.ttf`, reproducible with
`python verification/prepare-latin.py` (fontTools 4.55.3), SHA-256
`adbf4886a018c81066273f569b8ec612b5e75b883076a5aa4b1d33d5778d16d3`.
Derived from the pinned OFL Noto Bold below, with a new family/PostScript name
and STAT removed. All outline, hinting, advance and shaping tables are unchanged;
the script checks those table bytes. The original static face's STAT lists all
nine weights and four widths. Current Windows Skia reports it as weight100/width2
and adds synthetic bold for a 700 request, despite OS/2 specifying 700/5.
The derived face reports 700/5. Actual original GlowWord browser body screenshots
are pixel-identical before/after this metadata change. Both renderers now use
the same derived bytes; original bytes remain available for diagnosis. No
font-weight request or animation curve is weakened to compensate for this issue.

NotoSans-Bold.ttf is unmodified Noto Sans Bold from notofonts/noto-fonts,
commit ffebf8c1ee449e544955a7e813c54f9b73848eac,
hinted/ttf/NotoSans/NotoSans-Bold.ttf. SIL Open Font License 1.1: OFL.txt.

SHA-256: c976e4b1b99edc88775377fcc21692ca4bfa46b6d6ca6522bfda505b28ff9d6a.
Used only in comparison tests/harnesses; no SF Pro or reference sample media.
This font covers the Latin comparison; fallback-font screenshots do not prove parity.

NotoSansArabic-Bold.ttf is from the same pinned repository commit,
hinted/ttf/NotoSansArabic/NotoSansArabic-Bold.ttf; same SIL OFL 1.1 (OFL.txt).
SHA-256: ed2711b387750ae991b6a980b8dd16fdd65e6702b97e9f52adf3a8edf09ef4df.

FoliaParityCJK-Bold.otf is a renamed, verification-only subset of Noto Sans CJK SC Bold.
Original: notofonts/noto-cjk tag Sans2.004, commit
523d033d6cb47f4a80c58a35753646f5c3608a78,
Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Bold.otf.
Original SHA-256: b5f0d1a190a7f9b43c310a8850630af12553df32c4c050543f9059732d9b4c0a.
License from the same commit's LICENSE: SIL OFL 1.1, retained in CJK-OFL.txt;
original name-table copyright/license strings remain inside the subset.
Subset SHA-256: bba3466c258edbef68075a274af31b18697ac5afd61987b4170dc871d952174d.
Reproduction: verification/subset-cjk.py with fontTools 4.55.3; ASCII plus the
explicit synthetic CJK characters listed in that script. Family/PostScript names
changed to Folia Parity CJK / FoliaParityCJK-Bold; glyph outlines are unchanged.
This small subset is a comparison asset, not a general CJK application font.
