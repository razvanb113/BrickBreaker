import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Brick {
    int x,y,width,height;
    int health, maxHealth;
    protected boolean isVisible = true;
    protected BufferedImage image;
    protected BufferedImage crack1, crack2, crack3;

    public Brick(int x, int y, int width, int height, int health) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.health = health;
        this.maxHealth = health;
        if(health < 3) {this.loadImage("/sprites/caramida1.png");}
        else if(health < 6) {this.loadImage("/sprites/caramida2.png");}
        else this.loadImage("/sprites/caramida3.png");
        crack1 = getImage("/sprites/crack1.png");
        crack2 = getImage("/sprites/crack2.png");
        crack3 = getImage("/sprites/crack3.png");
    }

    public void loadImage(String fileName){

        try {
            image = ImageIO.read(getClass().getResource(fileName));
        } catch (IOException e) {
            image = null;
        }
    }

    public BufferedImage getImage(String fileName) {
        BufferedImage crackImage = null;
        try {
             crackImage = ImageIO.read(getClass().getResource(fileName));
        } catch (IOException e) {
        }
        return crackImage;
    }


    public void draw(Graphics g) {
        if (image == null) {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, width, height);
        } else {
            g.drawImage(image, x, y, width, height, null);
        }

        float healthRatio = (float) health / maxHealth;

        if (healthRatio <= 2f / 3f && healthRatio > 1f / 3f) {
            g.drawImage(crack1, x, y, width, height, null);
        } else if (healthRatio <= 1f / 3f && healthRatio > 0) {
            g.drawImage(crack2, x, y, width, height, null);
        } else if (health == 0) {
            g.drawImage(crack3, x, y, width, height, null);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}