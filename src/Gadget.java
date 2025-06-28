import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Gadget {
    //public enum types {MULTIPLIER, SPEED, PLATFORM};
    int type;
    BufferedImage image;
    int x, y;
    int width, height;
    private int duration; // în milisecunde

    public Gadget(int x, int y) {
        this.x = x;
        this.y = y;
        double variant = Math.random() * 10;
        this.width = 32;
        this.height = 32;

        if (variant < 3.3) {
            loadImage("/sprites/gadget_platform.png");
            type = 1;
            duration = 10000;
        } else if (variant < 6.6) {
            loadImage("/sprites/gadget_speed.png");
            type = 2;
            duration = 10000;
        } else {
            loadImage("/sprites/gadget_balls.png");
            type = 3;
        }

    }

    public void loadImage(String fileName){
        try {
            image = ImageIO.read(getClass().getResource(fileName));
        } catch (IOException e) {
            image = null;
        }
    }

    public void draw(Graphics g){
        if (image == null) {
            g.setColor(Color.RED);
            g.fillOval((int)x, (int)y, width, height);
        } else {
            g.drawImage(image, (int)x, (int)y, width, height, null);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getType(){
        return type;
    }

    public void update(){
        y += 2;
    }

    public int getDuration() {
        return duration;
    }
}
