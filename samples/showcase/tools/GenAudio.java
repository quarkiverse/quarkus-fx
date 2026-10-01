import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the showcase test audio : a C-E-G arpeggio, 16 bit PCM mono WAV at 22.05 kHz, 3.52 s.
 * usage: java tools/GenAudio.java output.wav
 */
public class GenAudio {

    public static void main(String[] args) throws IOException {
        int rate = 22050;
        int samples = 77616;
        double[] notes = { 523.25, 659.25, 783.99 };
        int perNote = samples / notes.length;

        ByteArrayOutputStream pcm = new ByteArrayOutputStream(samples * 2);
        for (int i = 0; i < samples; i++) {
            int note = Math.min(i / perNote, notes.length - 1);
            int t = i - note * perNote;
            // 20 ms attack, exponential decay
            double envelope = Math.min(1.0, t / (0.02 * rate)) * Math.exp(-2.0 * t / perNote);
            double value = 0.3 * envelope * Math.sin(2 * Math.PI * notes[note] * i / rate);
            short sample = (short) Math.round(value * Short.MAX_VALUE);
            pcm.write(sample & 0xFF);
            pcm.write((sample >> 8) & 0xFF);
        }

        byte[] data = pcm.toByteArray();
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes()).putInt(36 + data.length).put("WAVE".getBytes());
        header.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate * 2)
                .putShort((short) 2).putShort((short) 16);
        header.put("data".getBytes()).putInt(data.length);

        Path out = Path.of(args[0]);
        Files.write(out, header.array());
        Files.write(out, data, java.nio.file.StandardOpenOption.APPEND);
    }
}
