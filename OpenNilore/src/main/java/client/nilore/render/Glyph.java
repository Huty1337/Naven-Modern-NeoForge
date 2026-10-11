package client.nilore.render;

import client.nilore.render.GlyphPage;

record Glyph(int u, int v, int width, int height, char value, GlyphPage owner) {
}