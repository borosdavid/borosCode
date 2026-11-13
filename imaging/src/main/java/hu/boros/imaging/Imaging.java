package hu.boros.imaging;

import static org.springframework.http.HttpStatus.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@SpringBootApplication
@RestController
public class Imaging {
  public static void main(String[] args) {
    SpringApplication.run(Imaging.class, args);
  }

  @PostMapping(value = "/thumbnail", consumes = "image/jpeg", produces = "image/jpeg")
  public byte[] thumbnail(@RequestBody byte[] input) throws IOException {
    if (input.length > 20 * 1024 * 1024) throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
    try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw new IOException();
      var reader = readers.next();
      try {
        if (!reader.getFormatName().equalsIgnoreCase("JPEG")) throw new IOException();
        reader.setInput(stream);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if ((long) w * h > 100_000_000) throw new IOException();
        var params = reader.getDefaultReadParam();
        int sample = Math.max(1, Math.max(w, h) / 640);
        params.setSourceSubsampling(sample, sample, 0, 0);
        var original = reader.read(0, params);
        double scale = Math.min(1d, 420d / Math.max(w, h));
        var out =
            new BufferedImage(
                Math.max(1, (int) (w * scale)),
                Math.max(1, (int) (h * scale)),
                BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        try {
          g.setRenderingHint(
              RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          g.drawImage(original, 0, 0, out.getWidth(), out.getHeight(), null);
        } finally {
          g.dispose();
        }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", bytes);
        return bytes.toByteArray();
      } finally {
        reader.dispose();
      }
    } catch (IOException | IllegalArgumentException e) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Érvénytelen JPG.");
    }
  }
}
