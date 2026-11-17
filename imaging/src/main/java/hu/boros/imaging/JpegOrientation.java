package hu.boros.imaging;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Reads only the bounded EXIF orientation tag; the original JPEG is never changed. */
final class JpegOrientation {
  private JpegOrientation() {}

  static int read(byte[] jpeg) {
    try {
      var data = ByteBuffer.wrap(jpeg);
      if (Short.toUnsignedInt(data.getShort(0)) != 0xffd8) return 1;
      int pos = 2;
      while (pos + 4 <= jpeg.length) {
        int marker = Short.toUnsignedInt(data.getShort(pos));
        if ((marker & 0xff00) != 0xff00 || marker == 0xffda || marker == 0xffd9) return 1;
        int length = Short.toUnsignedInt(data.getShort(pos + 2));
        int end = pos + 2 + length;
        if (length < 2 || end > jpeg.length) return 1;
        if (marker == 0xffe1
            && length >= 16
            && data.getInt(pos + 4) == 0x45786966
            && data.getShort(pos + 8) == 0) {
          int tiff = pos + 10;
          int endian = Short.toUnsignedInt(data.getShort(tiff));
          if (endian != 0x4949 && endian != 0x4d4d) return 1;
          data.order(endian == 0x4949 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          if (data.getShort(tiff + 2) != 42) return 1;
          long offset = Integer.toUnsignedLong(data.getInt(tiff + 4));
          if (offset < 8 || offset > end - tiff - 2) return 1;
          int ifd = tiff + (int) offset;
          int count = Short.toUnsignedInt(data.getShort(ifd));
          for (int i = 0, entry = ifd + 2; i < count && entry <= end - 12; i++, entry += 12) {
            if (Short.toUnsignedInt(data.getShort(entry)) == 0x0112
                && data.getShort(entry + 2) == 3
                && data.getInt(entry + 4) == 1) {
              int orientation = Short.toUnsignedInt(data.getShort(entry + 8));
              return orientation >= 1 && orientation <= 8 ? orientation : 1;
            }
          }
          return 1;
        }
        pos = end;
      }
    } catch (IndexOutOfBoundsException ignored) {
      // Invalid or missing metadata must not stop decoding an otherwise valid JPEG.
    }
    return 1;
  }

  static BufferedImage apply(BufferedImage image, int orientation) {
    if (orientation <= 1 || orientation > 8) return image;
    int w = image.getWidth(), h = image.getHeight();
    var result =
        new BufferedImage(
            orientation >= 5 ? h : w, orientation >= 5 ? w : h, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int dx =
            switch (orientation) {
              case 2, 3 -> w - 1 - x;
              case 5, 8 -> y;
              case 6, 7 -> h - 1 - y;
              default -> x;
            };
        int dy =
            switch (orientation) {
              case 3, 4 -> h - 1 - y;
              case 5, 6 -> x;
              case 7, 8 -> w - 1 - x;
              default -> y;
            };
        result.setRGB(dx, dy, image.getRGB(x, y));
      }
    }
    return result;
  }
}
