import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class Main {

    static DecimalFormat                 df           = new DecimalFormat(".##");
    static Font                          f            = SwingUtils.DEFAULT_FONT;                               // SwingUtils.getFontOrDefault(new
                                                                                                               // File("C:/Users/Adam/Desktop/STIXv2.0.0/OTF/STIX2Math.otf"),
                                                                                                               // 50f);

    static final int                     charMax      = 0xFFFFF;
    static int                           char0        = 0x3C8;
    static int                           char1        = 0x3A9;
    static Shape                         shape0       = SwingUtils.getShapeFromString("" + (char) char0, 400f);
    static Shape                         shape1       = SwingUtils.getShapeFromString("" + (char) char1, 400f);
    static Function<Double, GeneralPath> blendedShape = blendShapes(shape0, shape1);
    // new Ellipse2D.Double(MyMath.randomDouble(20, 24),
    // MyMath.randomDouble(20,24),MyMath.randomDouble(0, 200),
    // MyMath.randomDouble(0, 200));

    static double                        time         = 0;
    static volatile boolean              toggleLoop   = false;
    static double                        dir          = 0.001;
    static JSlider                       slider       = null;
    static int                           max          = 10000;

    public static void main(String[] args) {

        // panel with drawring n stuff
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.clearRect(0, 0, g2d.getClipBounds().width, g2d.getClipBounds().height);
                g2d.setColor(Color.BLUE);
                g2d.translate(100, 500);
//                g2d.fill(blendedShape.apply((1 + Math.cos(Math.PI * time)) / 2d));
                drawShapeWithTimeLabels(g2d, blendedShape.apply((1 + Math.cos(Math.PI * time)) / 2d),
                        i -> i % 2 == 0 ? Color.RED : Color.BLUE);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(1000, 800);
            }
        };

        // create frame and initialize it
        JFrame frame = SwingUtils.newFrame(f -> {
            JPanel menu = new JPanel(new GridLayout(0, 1));
            JLabel titleLabel = new JLabel("Menu");
            titleLabel.setHorizontalTextPosition(JLabel.CENTER);
            titleLabel.setVerticalTextPosition(JLabel.CENTER);
            menu.add(titleLabel);
            menu.setBorder(BorderFactory.createDashedBorder(Color.RED));
            f.add(menu, BorderLayout.WEST);

            slider = SwingUtils.addSliderWithBinding(menu, 0, max, 0, i -> {
                time = (double) i / (double) max;
                panel.repaint();
            });
            JLabel char0Label = SwingUtils.addLabelToMenu(menu, "" + (char) char0);
            char0Label.setFont(SwingUtils.DEFAULT_FONT.deriveFont(50f));
            SwingUtils.addNumberSpinnerWithBinding(menu, char0, 0, charMax, 1, i -> {
                shape0 = SwingUtils.getShapeFromString("" + (char) (char0 = i), 400f);
                char0Label.setText("" + (char) char0);
                blendedShape = blendShapes(shape0, shape1);
                panel.repaint();
            });
            JLabel char1Label = SwingUtils.addLabelToMenu(menu, "" + (char) char1);
            char1Label.setFont(SwingUtils.DEFAULT_FONT.deriveFont(50f));
            SwingUtils.addNumberSpinnerWithBinding(menu, char1, 0, charMax, 1, i -> {
                shape1 = SwingUtils.getShapeFromString("" + (char) (char1 = i), 400f);
                char1Label.setText("" + (char) char1);
                blendedShape = blendShapes(shape0, shape1);
                panel.repaint();
            });

            f.add(panel, BorderLayout.CENTER);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
                public boolean dispatchKeyEvent(KeyEvent arg0) {
                    if (arg0.getKeyCode() == KeyEvent.VK_SPACE) {
                        if (arg0.getID() == KeyEvent.KEY_PRESSED) {
                            toggleLoop = !toggleLoop;
                        }
                        if (arg0.getID() == KeyEvent.KEY_RELEASED) {

                        }
                        return true;
                    }
                    return false;
                }
            });
        });

        ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1);
        stpe.scheduleAtFixedRate(new Thread(new Runnable() {
            public void run() {
                if (toggleLoop) {
                    if (time >= 1) {
                        dir *= -1;
                        time = 1;
                    } else if (time <= 0) {
                        dir *= -1;
                        time = 0;
                    }
                    time += dir;
                    slider.setValue((int) (time * max));
                    frame.repaint();
                }
            }
        }), 1, 1, TimeUnit.MILLISECONDS);
    }

    // TODO: make treat cubics as cubics and not as lines
    // TODO: handle multiple distinct contours instead of choosing first one
    // STRUCTURE:
    // <[x0,y0,x1,y1,x2,y2,code(MOVETO,LINETO...),distance,timeEnd].length:=9}>
    public static ArrayList<double[]> parameterizePath(Shape shape) {
        Rectangle2D bounds = shape.getBounds2D();
        Point2D.Double center = new Point2D.Double(bounds.getX() + bounds.getWidth() / 2d,
                bounds.getY() + bounds.getHeight() / 2d);
        boolean foundSuitableZero = false;
        double offset = 0;
        PathIterator path = shape.getPathIterator(null);
        ArrayList<double[]> segments = new ArrayList<>();
        double lastX = 0, lastY = 0, currX = 0, currY = 0;
        double[] coords = new double[6];
        int segmentCode;
        double totalDistance = 0;
        while (!path.isDone()) {
            double[] segment = new double[9];
            segment[6] = segmentCode = path.currentSegment(coords);
            for (int i = 0; i < coords.length; ++i)
                segment[i] = coords[i];
            switch (segmentCode) {
            case PathIterator.SEG_MOVETO:
                if (segments.size() > 0) {
                    while (!path.isDone())
                        path.next();
                    continue;
                }
                currX = coords[0];
                currY = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                currX = coords[0];
                currY = coords[1];
                segment[7] = Math.sqrt((currX - lastX) * (currX - lastX) + (currY - lastY) * (currY - lastY));
                if (!foundSuitableZero && Math.signum(currY - center.y) != Math.signum(lastY - center.y)) {
                    foundSuitableZero = true;
                    offset = totalDistance;// TODO: put at some point in the center of the line
                }
                break;
            case PathIterator.SEG_QUADTO:
                // Bezier_2(t)=(1-t)^2 P0 + 2(1-t)t P1 + t^2 P2
                currX = coords[2];
                currY = coords[3];
                double a = (currX - 2 * coords[0] + lastX) * (currX - 2 * coords[0] + lastX)
                        + (currY - 2 * coords[1] + lastY) * (currY - 2 * coords[1] + lastY);
                double b = (currX - 2 * coords[0] + lastX) * (coords[0] - currX)
                        + (currY - 2 * coords[1] + lastY) * (coords[1] - currY);
                double c = (coords[0] - currX) * (coords[0] - currX) + (coords[1] - currY) * (coords[1] - currY);
                if (Math.abs(
                        (currX - lastX) * (coords[1] - lastY) - (currY - lastY) * (coords[0] - lastX)) < 0.00000000001)
                    segment[7] = Math.sqrt((currX - lastX) * (currX - lastX) + (currY - lastY) * (currY - lastY));
                else
                    segment[7] = ((a + b) * Math.sqrt(c + 2 * b + a) - b * Math.sqrt(c)
                            + (a * c - b * b) / Math.sqrt(a)
                                    * Math.log((a + b + Math.sqrt(a * (c + 2 * b + a))) / (b + Math.sqrt(a * c))))
                            / a; {
                int pmCurr = (int) Math.signum(currY - center.y);
                int pmLast = (int) Math.signum(lastY - center.y);
                int pmCtrl = (int) Math.signum(coords[1] - center.y);
                if (!foundSuitableZero && (pmCurr != pmLast || pmLast != pmCtrl || pmCtrl != pmCurr)) {
                    foundSuitableZero = true;
                    offset = totalDistance;// TODO: put at some point in the center of the line
                }
            }
                break;
            case PathIterator.SEG_CUBICTO:
                // TODO: handle cubics
                currX = coords[4];
                currY = coords[5];
                segment[7] = Math.sqrt((currX - lastX) * (currX - lastX) + (currY - lastY) * (currY - lastY));
                System.out.println("CUBICAHAHAHAHHAHAH");
                break;
            default:
            case PathIterator.SEG_CLOSE:
                break;
            }
            totalDistance += segment[7];
            segments.add(segment);
            lastX = currX;
            lastY = currY;
            path.next();
        }
        double currTime = 0;
        for (int i = 0; i < segments.size(); ++i) {
            currTime += segments.get(i)[7] / totalDistance;
            segments.get(i)[8] = currTime;
        }
//        double currDist = 0;
//        for (int i = 0; i < segments.size(); ++i) {
//            currDist += segments.get(i)[7];
//            segments.get(i)[8] = ((totalDistance + currDist - offset) % totalDistance) / totalDistance;
//        }
        return segments;
    }

    // TODO: implement cubics
    public static Function<Double, GeneralPath> blendShapes(Shape shape0, Shape shape1) {
        ArrayList<double[]> contour0 = parameterizePath(shape0);
        ArrayList<double[]> contour1 = parameterizePath(shape1);

        GeneralPath p = new GeneralPath();
        return new Function<Double, GeneralPath>() {
            public GeneralPath apply(Double t) {
                p.reset();
                // keep track of current segment in contour 0 and contour 1
                Iterator<double[]> it0 = contour0.iterator(), it1 = contour1.iterator();
                double[] segment0 = it0.next(), segment1 = it1.next();
                while (it0.hasNext() && it1.hasNext()) {
                    // add combination of segments; different combination algorithm depending on
                    // what type of segment
                    switch (Math.max((int) segment0[6], (int) segment1[6])) {
                    case PathIterator.SEG_MOVETO:
                        p.moveTo((1 - t) * segment0[0] + t * segment1[0], (1 - t) * segment0[1] + t * segment1[1]);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        // TODO: CHANGE TO CUBIC
                        // p.lineTo(x, y);
                        System.out.println("CUBICAHAHAHAHHAHAH");
                        break;
                    case PathIterator.SEG_QUADTO:
                        // quadTo(control x, control y, final x, final y)
                        if (segment0[6] < PathIterator.SEG_QUADTO) {
                            p.quadTo((1 - t) * segment0[0] + t * segment1[0], (1 - t) * segment0[1] + t * segment1[1],
                                    (1 - t) * segment0[0] + t * segment1[2], (1 - t) * segment0[1] + t * segment1[3]);
                        } else if (segment1[6] < PathIterator.SEG_QUADTO) {
                            p.quadTo((1 - t) * segment0[0] + t * segment1[0], (1 - t) * segment0[1] + t * segment1[1],
                                    (1 - t) * segment0[2] + t * segment1[0], (1 - t) * segment0[3] + t * segment1[1]);
                        } else
                            p.quadTo((1 - t) * segment0[0] + t * segment1[0], (1 - t) * segment0[1] + t * segment1[1],
                                    (1 - t) * segment0[2] + t * segment1[2], (1 - t) * segment0[3] + t * segment1[3]);
                        break;
                    case PathIterator.SEG_LINETO:
                        p.lineTo((1 - t) * segment0[0] + t * segment1[0], (1 - t) * segment0[1] + t * segment1[1]);
                        break;
                    case PathIterator.SEG_CLOSE:
                    default:
                        p.closePath();
                        break;
                    }
                    // look at end time for each current segment in contours 0 and 1
                    // if equal: jump to next segment in each contour
                    // if unequal: jump to next segment in smallest time ending contour
                    switch ((int) Math.signum(segment0[8] - segment1[8])) {
                    case 0:
                    default:
                        segment0 = it0.next();
                        segment1 = it1.next();
                        break;
                    case -1:
                        segment0 = it0.next();
                        break;
                    case 1:
                        segment1 = it1.next();
                        break;
                    }
                }

                return p;
            }
        };
    }

    // TODO: Support cubics
    public static void drawShapeWithTimeLabels(Graphics2D g2d, Shape shape, IntFunction<Color> colorOfEachSegment) {
        int segmentIndex = 0;
        double lastX = 0, lastY = 0, currX = 0, currY = 0;
        double[] coords = new double[6];
        int segmentCode;
        for (double[] segment : parameterizePath(shape)) {
            g2d.setColor(colorOfEachSegment.apply(segmentIndex++));
            for (int i = 0; i < coords.length; ++i)
                coords[i] = segment[i];
            segmentCode = (int) segment[6];
            switch (segmentCode) {
            case PathIterator.SEG_MOVETO:
                currX = coords[0];
                currY = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                currX = coords[0];
                currY = coords[1];
                g2d.drawLine((int) lastX, (int) lastY, (int) currX, (int) currY);
                break;
            case PathIterator.SEG_QUADTO:
                // Bezier_2(t)=(1-t)^2 P0 + 2(1-t)t P1 + t^2 P2
                currX = coords[2];
                currY = coords[3];
                if (Math.abs(
                        (currX - lastX) * (coords[1] - lastY) - (currY - lastY) * (coords[0] - lastX)) < 0.0000000001)
                    g2d.drawLine((int) lastX, (int) lastY, (int) currX, (int) currY);
                else {
                    double step = 1d / 400d;
                    double t = 0, t_last;
                    double t0, t1, t2, t0_last, t1_last, t2_last;
                    do {
                        t_last = t;
                        t += step;
                        t0 = t * t;
                        t1 = t * (1 - t);
                        t2 = t0 + 1 - 2 * t;// (1-t)^2=1+t^2-2t
                        t0_last = t_last * t_last;
                        t1_last = t_last * (1 - t_last);
                        t2_last = t0_last + 1 - 2 * t_last;// (1-t)^2=1+t^2-2t
                        g2d.drawLine((int) (t0_last * lastX + 2 * t1_last * coords[0] + t2_last * currX),
                                (int) (t0_last * lastY + 2 * t1_last * coords[1] + t2_last * currY),
                                (int) (t0 * lastX + 2 * t1 * coords[0] + t2 * currX),
                                (int) (t0 * lastY + 2 * t1 * coords[1] + t2 * currY));
                    } while (t < 1);
                }
                break;
            case PathIterator.SEG_CUBICTO:
                // TODO: handle cubic correctly
                currX = coords[4];
                currY = coords[5];
                g2d.drawLine((int) lastX, (int) lastY, (int) currX, (int) currY);
                System.out.println("CUBICAHAHAHAHHAHAH");
                break;
            default:
            case PathIterator.SEG_CLOSE:
                g2d.fillOval((int) (lastX - 2), (int) (lastY - 2), 4, 4);
                break;
            }
            g2d.drawString("" + df.format(segment[8]), (int) currX, (int) currY);
            lastX = currX;
            lastY = currY;
        }
    }

}
