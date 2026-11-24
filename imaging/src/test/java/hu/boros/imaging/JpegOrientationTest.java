package hu.boros.imaging;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class JpegOrientationTest {
  @Test
  void readsBothEndianTagsAndRejectsInvalidOffsets() {
    for (var order : new ByteOrder[] {ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
      var bytes = ByteBuffer.allocate(40);
      bytes.putShort((short) 0xffd8).putShort((short) 0xffe1).putShort((short) 34);
      bytes.putInt(0x45786966).putShort((short) 0);
      bytes.order(order).putShort((short) (order == ByteOrder.BIG_ENDIAN ? 0x4d4d : 0x4949));
      bytes.putShort((short) 42).putInt(8).putShort((short) 1);
      bytes.putShort((short) 0x112).putShort((short) 3).putInt(1).putShort((short) 6);
      assertEquals(6, JpegOrientation.read(bytes.array()));
      bytes.putInt(16, Integer.MAX_VALUE);
      assertEquals(1, JpegOrientation.read(bytes.array()));
    }
    assertEquals(1, JpegOrientation.read(new byte[] {(byte) 0xff}));
  }

  @Test
  void rotatesAndMirrorsAllEightOrientations() {
    var source = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
    int color = 1;
    for (int y = 0; y < 3; y++) for (int x = 0; x < 2; x++) source.setRGB(x, y, color++);
    int[][] expected = {
      {1, 2, 3, 4, 5, 6}, {2, 1, 4, 3, 6, 5}, {6, 5, 4, 3, 2, 1}, {5, 6, 3, 4, 1, 2},
      {1, 3, 5, 2, 4, 6}, {5, 3, 1, 6, 4, 2}, {6, 4, 2, 5, 3, 1}, {2, 4, 6, 1, 3, 5}
    };
    for (int orientation = 1; orientation <= 8; orientation++) {
      var out = JpegOrientation.apply(source, orientation);
      assertEquals(orientation >= 5 ? 3 : 2, out.getWidth());
      int index = 0;
      for (int y = 0; y < out.getHeight(); y++) {
        for (int x = 0; x < out.getWidth(); x++) {
          assertEquals(expected[orientation - 1][index++], out.getRGB(x, y) & 0xffffff);
        }
      }
    }
  }
}
