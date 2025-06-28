import javax.swing.*;
import java.awt.*;

public class GadgetTime {
    private Gadget gadget;
    private long startTime;
    private int duration;

    public GadgetTime(Gadget g) {
        this.gadget = g;
        this.duration = g.getDuration(); // presupune că există getDuration()
        this.startTime = System.currentTimeMillis();

        Timer timer = new Timer(duration, e -> {
            if (onEnd != null) onEnd.run();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public Runnable onEnd;
    private String type;

    public void setType(String t) { this.type = t; }

    public boolean isExpired() {
        return getRemaining() <= 0;
    }

    public int getRemaining() {
        long elapsed = System.currentTimeMillis() - startTime;
        return (int)Math.max(0, duration - elapsed);
    }

    public boolean refreshIfSameType(Gadget g) {
        if (g.getType() == gadget.getType()) {
            this.startTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public void draw(Graphics2D g, int index) {
        int size = 40;
        int x = 10;
        int y = 10 + index * (size + 5);

        if (gadget.image != null)
            g.drawImage(gadget.image, x + 4, y + 4, size - 8, size - 8, null);
        else {
            g.setColor(Color.GRAY);
            g.fillOval(x + 4, y + 4, size - 8, size - 8);
        }

        float progress = getRemaining() / (float) duration;
        g.setStroke(new BasicStroke(3));
        g.setColor(Color.GREEN);
        g.drawArc(x, y, size, size, 90, -(int)(360 * progress));
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
    }

    public Gadget getGadget() {
        return this.gadget;
    }
}
