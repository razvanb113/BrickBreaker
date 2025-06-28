import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

public class StartArrow {
    private int ballX, ballY;
    private int px, py;

    AffineTransform tx = new AffineTransform();
    Line2D.Double line;

    Polygon arrowHead = new Polygon();

    public StartArrow(int ballX, int ballY, int px, int py, Graphics2D g2d) {
        this.ballX = ballX;
        this.ballY = ballY;
        this.px = px;
        this.py = py;

        line = new Line2D.Double(ballX,ballY, px, py);
        tx.setToIdentity();
        double angle = Math.atan2(line.y2-line.y1, line.x2-line.x1);
        tx.translate(line.x2, line.y2);
        tx.rotate((angle-Math.PI/2d));

        Graphics2D g = (Graphics2D) g2d.create();
        g.setTransform(tx);
        g.fill(arrowHead);
        g.dispose();
    }
}
