package hu.boros.imaging;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MobilePhotoTest {
  private byte[] jpeg() throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "jpg", bytes);
    return bytes.toByteArray();
  }

  @Test
  void rotatesThumbnailAndPreservesOriginal() throws Exception {
    byte[] original = jpeg();
    var exif = ByteBuffer.allocate(36);
    exif.putShort((short) 0xffe1).putShort((short) 34).putInt(0x45786966).putShort((short) 0);
    exif.putShort((short) 0x4d4d).putShort((short) 42).putInt(8).putShort((short) 1);
    exif.putShort((short) 0x112).putShort((short) 3).putInt(1).putShort((short) 6);
    var combined = new ByteArrayOutputStream();
    combined.write(original, 0, 2);
    combined.write(exif.array());
    combined.write(original, 2, original.length - 2);
    byte[] input = combined.toByteArray(), copy = input.clone();
    var thumbnail = ImageIO.read(new ByteArrayInputStream(new Imaging().thumbnail(input)));
    assertEquals(4, thumbnail.getWidth());
    assertEquals(8, thumbnail.getHeight());
    assertArrayEquals(copy, input);
  }

  @Test
  void acceptsFortyMiBAndRejectsLargerOrInvalidImages() throws Exception {
    var imaging = new Imaging();
    assertNotNull(imaging.thumbnail(Arrays.copyOf(jpeg(), 40 * 1024 * 1024)));
    var tooLarge =
        assertThrows(
            ResponseStatusException.class, () -> imaging.thumbnail(new byte[40 * 1024 * 1024 + 1]));
    assertEquals(413, tooLarge.getStatusCode().value());
    var invalid =
        assertThrows(ResponseStatusException.class, () -> imaging.thumbnail(new byte[] {1, 2, 3}));
    assertEquals(422, invalid.getStatusCode().value());
  }
}
