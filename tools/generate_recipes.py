#!/usr/bin/env python3
"""Generates the multiitemframe crafting recipe JSON files for both loaders.

Forge (1.20.1) uses the legacy recipe schema (`{"item": ..., "count": ...}`
results, `{"item": "..."}` ingredients). NeoForge (1.21.1) uses the schema
introduced in MC 1.21 (`{"id": ..., "count": ...}` results, plain-string
ingredients). Recipes are pure data, but this schema difference means they
still need to be generated per loader, same as the Ch.2 Java code.

Run: python tools/generate_recipes.py
"""
import json
import os

MOD_ID = "multiitemframe"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Forge targets 1.20.1, which uses the pre-1.21 plural folder name ("recipes"); NeoForge
# targets 1.21.1, which uses the singular name introduced in 1.21 ("recipe").
FORGE_DIR = os.path.join(ROOT, "forge", "src", "main", "resources", "data", MOD_ID, "recipes")
NEOFORGE_DIR = os.path.join(ROOT, "neoforge", "src", "main", "resources", "data", MOD_ID, "recipe")


def mid(path):
    return path if ":" in path else f"{MOD_ID}:{path}"


def write(directory, name, data):
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, f"{name}.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def result(item, count=1, legacy=True):
    return {"item": mid(item), "count": count} if legacy else {"id": mid(item), "count": count}


def ingredient(item, legacy=True):
    return {"item": mid(item)} if legacy else mid(item)


def shapeless(inputs, out, count=1, legacy=True):
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [ingredient(i, legacy) for i in inputs],
        "result": result(out, count, legacy),
    }


def shaped(pattern, key, out, count=1, legacy=True):
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": {k: ingredient(v, legacy) for k, v in key.items()},
        "result": result(out, count, legacy),
    }


def build_recipes(legacy):
    recipes = {}

    recipes["frame_1x1"] = shapeless(["minecraft:item_frame", "minecraft:redstone"], "frame_1x1", legacy=legacy)

    recipes["frame_1x2"] = shaped(["@", "@"], {"@": "frame_1x1"}, "frame_1x2", legacy=legacy)
    recipes["frame_2x1"] = shaped(["@@"], {"@": "frame_1x1"}, "frame_2x1", legacy=legacy)
    recipes["frame_1x2_vice_versa"] = shapeless(["frame_2x1"], "frame_1x2", legacy=legacy)
    recipes["frame_2x1_vice_versa"] = shapeless(["frame_1x2"], "frame_2x1", legacy=legacy)

    recipes["frame_1and2"] = shaped(["@ ", "@@"], {"@": "frame_1x1"}, "frame_1and2", legacy=legacy)
    recipes["frame_1and2_alt"] = shaped(["@", "&"], {"@": "frame_1x1", "&": "frame_1x2"}, "frame_1and2", legacy=legacy)

    recipes["frame_2and1"] = shaped(["@@", "@ "], {"@": "frame_1x1"}, "frame_2and1", legacy=legacy)
    recipes["frame_2and1_alt"] = shaped(["&", "@"], {"&": "frame_1x2", "@": "frame_1x1"}, "frame_2and1", legacy=legacy)

    recipes["frame_2x2"] = shaped(["@@", "@@"], {"@": "frame_1x1"}, "frame_2x2", legacy=legacy)
    recipes["frame_2x2_from_1x2"] = shaped(["&&"], {"&": "frame_1x2"}, "frame_2x2", legacy=legacy)
    recipes["frame_2x2_from_2x1"] = shaped(["$", "$"], {"$": "frame_2x1"}, "frame_2x2", legacy=legacy)

    # Glow frames can be crafted directly from vanilla ingredients (mirroring frame_1x1's own
    # recipe, plus glowstone dust)...
    recipes["glow_frame_1x1"] = shapeless(
        ["minecraft:item_frame", "minecraft:redstone", "minecraft:glowstone_dust"], "glow_frame_1x1", legacy=legacy)

    recipes["glow_frame_1x2"] = shaped(["@", "@"], {"@": "glow_frame_1x1"}, "glow_frame_1x2", legacy=legacy)
    recipes["glow_frame_2x1"] = shaped(["@@"], {"@": "glow_frame_1x1"}, "glow_frame_2x1", legacy=legacy)
    recipes["glow_frame_1x2_vice_versa"] = shapeless(["glow_frame_2x1"], "glow_frame_1x2", legacy=legacy)
    recipes["glow_frame_2x1_vice_versa"] = shapeless(["glow_frame_1x2"], "glow_frame_2x1", legacy=legacy)

    recipes["glow_frame_1and2"] = shaped(["@ ", "@@"], {"@": "glow_frame_1x1"}, "glow_frame_1and2", legacy=legacy)
    recipes["glow_frame_1and2_alt"] = shaped(["@", "&"], {"@": "glow_frame_1x1", "&": "glow_frame_1x2"}, "glow_frame_1and2", legacy=legacy)

    recipes["glow_frame_2and1"] = shaped(["@@", "@ "], {"@": "glow_frame_1x1"}, "glow_frame_2and1", legacy=legacy)
    recipes["glow_frame_2and1_alt"] = shaped(["&", "@"], {"&": "glow_frame_1x2", "@": "glow_frame_1x1"}, "glow_frame_2and1", legacy=legacy)

    recipes["glow_frame_2x2"] = shaped(["@@", "@@"], {"@": "glow_frame_1x1"}, "glow_frame_2x2", legacy=legacy)
    recipes["glow_frame_2x2_from_1x2"] = shaped(["&&"], {"&": "glow_frame_1x2"}, "glow_frame_2x2", legacy=legacy)
    recipes["glow_frame_2x2_from_2x1"] = shaped(["$", "$"], {"$": "glow_frame_2x1"}, "glow_frame_2x2", legacy=legacy)

    # ...or upgraded in-place from an already-built non-glow frame of the same size, one
    # glowstone dust each (mirrors how a vanilla Item Frame is turned into a Glow Item Frame).
    for size in ("1x1", "1x2", "2x1", "1and2", "2and1", "2x2"):
        recipes[f"glow_frame_{size}_from_nonglow"] = shapeless(
            [f"frame_{size}", "minecraft:glowstone_dust"], f"glow_frame_{size}", legacy=legacy)

    return recipes


def main():
    for directory, legacy in ((FORGE_DIR, True), (NEOFORGE_DIR, False)):
        recipes = build_recipes(legacy)
        for name, data in recipes.items():
            write(directory, name, data)
        print(f"Wrote {len(recipes)} recipes to {directory}")


if __name__ == "__main__":
    main()
