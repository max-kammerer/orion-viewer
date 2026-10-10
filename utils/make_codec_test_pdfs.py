#!/usr/bin/env python3
"""
Builds the one-page PDFs in orion-viewer/src/androidTest/assets/testData/codecs: page images in
the codecs scanned books use (JPEG 2000, CCITT G4, JBIG2), kept as they are in the PDF, so the
decoders and their caching paths get exercised.

Pages are 600x800 pt and images are drawn 1 px = 1 pt: at zoom 1.0 nothing is resampled and the
tiled render can be compared with the full one pixel for pixel (CodecRenderTest).

Needs Pillow (with OpenJPEG) and, for the JBIG2 page, a host mutool and jbig2dec's annex-h.jbig2
(the example bitstream of ITU-T T.88 Annex H):

    utils/make_codec_test_pdfs.py OUT_DIR [--mutool PATH --annex-h PATH]
"""
import argparse
import io
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw, TiffImagePlugin

W, H = 600, 800


def write_pdf(path, image_dict, image_data, content, font=False):
    """A minimal PDF: catalog, pages, one page with the image as /Im0 (and Helvetica as /F1)."""
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %d %d] /Contents 4 0 R "
         "/Resources << /XObject << /Im0 5 0 R >>%s >> >>" % (W, H, " /Font << /F1 6 0 R >>" if font else "")).encode(),
        b"<< /Length %d >>\nstream\n" % len(content) + content + b"\nendstream",
        image_dict + b"\nstream\n" + image_data + b"\nendstream",
    ]
    if font:
        objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    out = io.BytesIO()
    out.write(b"%PDF-1.7\n%\xe2\xe3\xcf\xd3\n")
    offsets = []
    for i, body in enumerate(objects, 1):
        offsets.append(out.tell())
        out.write(b"%d 0 obj\n" % i + body + b"\nendobj\n")
    xref = out.tell()
    out.write(b"xref\n0 %d\n0000000000 65535 f \n" % (len(objects) + 1))
    for off in offsets:
        out.write(b"%010d 00000 n \n" % off)
    out.write(b"trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n" % (len(objects) + 1, xref))
    with open(path, "wb") as f:
        f.write(out.getvalue())


def picture(mode):
    """Text lines, boxes and a gradient: edges and flat areas on both sides of the tile seams."""
    im = Image.new(mode, (W, H), "white")
    d = ImageDraw.Draw(im)
    for x in range(W):
        shade = x * 255 // (W - 1)
        d.line([(x, 0), (x, 90)], fill=(shade, 255 - shade, 128) if mode == "RGB" else shade)
    for i, y in enumerate(range(130, H - 40, 34)):
        d.text((30 + (i % 3) * 7, y), "Line %02d: the quick brown fox jumps over the lazy dog" % i,
               fill=(20, 40, 160) if mode == "RGB" else 0)
    d.rectangle([280, 380, 320, 420], outline=(200, 0, 0) if mode == "RGB" else 0, width=3)
    return im


def jp2_bytes(im):
    buf = io.BytesIO()
    # Lossy (9/7 wavelet, ~1:20) keeps the files small; decoding is still deterministic.
    im.save(buf, "JPEG2000", irreversible=True, quality_mode="rates", quality_layers=[20])
    return buf.getvalue()


def make_jpx(out_dir):
    content = b"q %d 0 0 %d 0 0 cm /Im0 Do Q" % (W, H)
    gray = picture("L")
    write_pdf(os.path.join(out_dir, "jpx_gray.pdf"),
              b"<< /Type /XObject /Subtype /Image /Width %d /Height %d /ColorSpace /DeviceGray "
              b"/BitsPerComponent 8 /Filter /JPXDecode /Length %d >>" % (W, H, len(jp2_bytes(gray))),
              jp2_bytes(gray), content)
    rgb = picture("RGB")
    write_pdf(os.path.join(out_dir, "jpx_rgb.pdf"),
              b"<< /Type /XObject /Subtype /Image /Width %d /Height %d /ColorSpace /DeviceRGB "
              b"/BitsPerComponent 8 /Filter /JPXDecode /Length %d >>" % (W, H, len(jp2_bytes(rgb))),
              jp2_bytes(rgb), content)
    # Alpha in the code stream (SMaskInData): text drawn first shows through the transparent stripes.
    rgba = picture("RGB").convert("RGBA")
    alpha = Image.new("L", (W, H), 255)
    ImageDraw.Draw(alpha).rectangle([0, 300, W, 500], fill=0)
    for x in range(0, W, 80):
        ImageDraw.Draw(alpha).rectangle([x, 500, x + 39, 700], fill=0)
    rgba.putalpha(alpha)
    data = jp2_bytes(rgba)
    under = b"BT /F1 40 Tf 40 380 Td (Text under the image) Tj ET\nBT /F1 40 Tf 40 160 Td (Seen through stripes) Tj ET\n"
    write_pdf(os.path.join(out_dir, "jpx_alpha.pdf"),
              b"<< /Type /XObject /Subtype /Image /Width %d /Height %d /ColorSpace /DeviceRGB "
              b"/BitsPerComponent 8 /SMaskInData 1 /Filter /JPXDecode /Length %d >>" % (W, H, len(data)),
              data, under + content, font=True)


def make_ccitt(out_dir):
    im = picture("L").point(lambda v: 255 if v > 160 else 0).convert("1")
    buf = io.BytesIO()
    im.save(buf, "TIFF", compression="group4", tiffinfo={TiffImagePlugin.ROWSPERSTRIP: H})
    tif = Image.open(io.BytesIO(buf.getvalue()))
    offsets, counts = tif.tag_v2[TiffImagePlugin.STRIPOFFSETS], tif.tag_v2[TiffImagePlugin.STRIPBYTECOUNTS]
    assert len(offsets) == 1, "expected a single strip"
    data = buf.getvalue()[offsets[0]:offsets[0] + counts[0]]
    # Photometric MinIsWhite (0): 1 bits are black, which /BlackIs1 true says; MinIsBlack: the reverse.
    photometric = tif.tag_v2.get(TiffImagePlugin.PHOTOMETRIC_INTERPRETATION, 0)
    black_is_1 = b"false" if photometric == 0 else b"true"
    write_pdf(os.path.join(out_dir, "ccitt_g4.pdf"),
              b"<< /Type /XObject /Subtype /Image /Width %d /Height %d /ColorSpace /DeviceGray "
              b"/BitsPerComponent 1 /Filter /CCITTFaxDecode /DecodeParms << /K -1 /Columns %d /Rows %d "
              b"/BlackIs1 %s >> /Length %d >>" % (W, H, W, H, black_is_1, len(data)),
              data, b"q %d 0 0 %d 0 0 cm /Im0 Do Q" % (W, H))


def make_jbig2(out_dir, mutool, annex_h):
    # mutool turns the standalone file into an embedded JBIG2 stream (page 1, 64x56). It is drawn
    # 1:1 three times: inside a tile, and across the seams between the four tiles of the page.
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as page:
        page.write("%%%%MediaBox 0 0 %d %d\n%%%%Image Im0 %s\n" % (W, H, annex_h))
        for x, y in ((20, 700), (268, 372), (500, 50)):
            page.write("q 64 0 0 56 %d %d cm /Im0 Do Q\n" % (x, y))
    try:
        subprocess.run([mutool, "create", "-o", os.path.join(out_dir, "jbig2_annex_h.pdf"), page.name], check=True)
    finally:
        os.unlink(page.name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("out_dir")
    parser.add_argument("--mutool")
    parser.add_argument("--annex-h")
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    make_jpx(args.out_dir)
    make_ccitt(args.out_dir)
    if args.mutool and args.annex_h:
        make_jbig2(args.out_dir, args.mutool, args.annex_h)
    else:
        print("skipping the JBIG2 page: --mutool and --annex-h not given", file=sys.stderr)


if __name__ == "__main__":
    main()
