import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class Platform {
    private int x, y;
    private int width;
    private int height;
    private Color color = Color.WHITE;
    long extendedUntil;
    boolean gadgetApplied = false;

    public Platform(int x, int y) {
        this.x = x;
        this.y = y;
        width = 100;
        height = 20;
    }

    public void extend(int duration) {
        gadgetApplied = true;
        this.width = 250;
        extendedUntil = System.currentTimeMillis() + duration * 1000L;
    }

    public void draw(Graphics g) {
        if (gadgetApplied) {
            if (System.currentTimeMillis() > extendedUntil) {
                this.width = 100;
                gadgetApplied = false;
            }
        }
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setX(int x) {
        this.x = x;
    }


    public int getX() {
        return x;
    }
}
