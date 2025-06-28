import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Ball {
    private float x, y;
    private float dx, dy;
    private BufferedImage image;
    private int width, height;

    public Ball(int x, int y, float dx, float dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        loadImage("/sprites/ball.png");
        this.width = 16;
        this.height = 16;
    }

    public void update() {
        x += dx;
        y += dy;

        if (x < 0) {
            x = 0;
            dx *= -1;
        } else if (x + width > GameBoard.BOARD_WIDTH - 16) {
            x = GameBoard.BOARD_WIDTH - 16 - width;
            dx *= -1;
        }

        if (y < 0) {
            y = 0;
            dy *= -1;
        } else if (y + height > GameBoard.BOARD_HEIGHT - 40) {
            y = GameBoard.BOARD_HEIGHT - 40 - height;
            dy *= -1;
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
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

    public float getX() { return x; }
    public float getY() { return y; }
    public float getDx() { return dx; }
    public void setDx(float dx) { this.dx = dx; }
    public float getDy() { return dy; }
    public void setDy(float dy) { this.dy = dy; }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
