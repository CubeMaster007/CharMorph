import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SwingUtils {

    public static final String            NAME_MENU                 = "MENU";

    public static final String            DEFAULT_TITLE             = "Test Window: " + System.getProperty("user.name");
    public static final Font              DEFAULT_FONT              = new JLabel().getFont();
    // newFontRenderContext(AffineTransform, isAntiAliased, usesFractionalMetrics)
    public static final FontRenderContext DEFAULT_FONTRENDERCONTEXT = new FontRenderContext(null, false, false);

    private SwingUtils() {
    }

    // METHODS TO CONSTRUCT JFRAME
    public static JFrame newFrame(Consumer<JFrame> preInit, String title) {
        JFrame frame = new JFrame(title);
        preInit.accept(frame);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        return frame;
    }

    public static JFrame newFrame(Consumer<JFrame> preInit) {
        return newFrame(preInit, DEFAULT_TITLE);
    }

    public static JFrame newFrame(String title) {
        return newFrame(t -> {
        }, title);
    }

    public static JFrame newFrame() {
        return newFrame(t -> {
        }, DEFAULT_TITLE);
    }
    // METHODS TO CONSTRUCT JFRAME

    // METHODS TO ADD COMPONENTS
    public static JLabel addLabelToMenu(Container container, String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        container.add(label);
        return label;
    }
    // METHODS TO ADD COMPONENTS

    // METHODS TO BIND ACTIONS TO COMPONENTS
    public static void addKeyBinding(Component component, KeyStroke event, AbstractAction action, Object key) {
        if (component instanceof JComponent) {
            ((JComponent) component).getInputMap().put(event, key);
            ((JComponent) component).getActionMap().put(key, action);
        } else {
            component.addKeyListener(new KeyListener() {
                public void keyTyped(KeyEvent e) {
                    if (KeyStroke.getKeyStrokeForEvent(e).equals(event))
                        action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
                }

                public void keyReleased(KeyEvent e) {
                    if (KeyStroke.getKeyStrokeForEvent(e).equals(event))
                        action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
                }

                public void keyPressed(KeyEvent e) {
                    if (KeyStroke.getKeyStrokeForEvent(e).equals(event))
                        action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
                }
            });
        }
    }

    public static void addKeyBinding(Component component, int keyCode, AbstractAction action, Object key) {
        addKeyBinding(component, KeyStroke.getKeyStroke(keyCode, 0), action, key);
    }

    public static void addKeyBinding(Component component, int keyCode, Consumer<ActionEvent> action, Object key) {
        addKeyBinding(component, KeyStroke.getKeyStroke(keyCode, 0), new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                action.accept(arg0);
            }
        }, key);
    }

    /**
     * Assumes that the given {@link AbstractAction} is unique and uses it as a key
     * in the hashmaps
     * 
     * @param component
     * @param keyCode
     * @param action
     */
    public static void addKeyBinding(Component component, int keyCode, AbstractAction action) {
        addKeyBinding(component, keyCode, action, action);
    }

    /**
     * Assumes that the given {@link Consumer} is unique and uses it as a key in the
     * hashmaps
     * 
     * @param component
     * @param keyCode
     * @param action
     */
    public static void addKeyBinding(Component component, int keyCode, Consumer<ActionEvent> action) {
        addKeyBinding(component, keyCode, action, action);
    }

    /**
     * Passes the negation of {@link MouseWheelEvent#getPreciseWheelRotation()} into
     * the given {@link DoubleConsumer}
     * 
     * @param component
     * @param action
     */
    public static void addWheelBinding(JComponent component, DoubleConsumer action) {
        component.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent arg0) {
                action.accept(-arg0.getPreciseWheelRotation());
            }
        });
    }

    public static JSpinner addSpinnerWithBinding(Container container, SpinnerModel model,
            ChangeListener changeListener) {
        JSpinner spinner = new JSpinner(model);
        container.add(spinner);
        spinner.addChangeListener(changeListener);
        return spinner;
    }

    public static JSpinner addNumberSpinnerWithBinding(Container container, int curr, int min, int max, int inc,
            IntConsumer changeListener) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(curr, min, max, inc));
        container.add(spinner);
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Object val = spinner.getValue();
                if (val instanceof Integer)
                    changeListener.accept((Integer) val);
            }
        });
        return spinner;
    }

    public static JSlider addSliderWithBinding(Container container, int orientation, int min, int max, int value,
            ChangeListener changeListener) {
        JSlider slider = new JSlider(orientation, min, max, value);
        container.add(slider);
        slider.addChangeListener(changeListener);
        return slider;
    }

    public static JSlider addSliderWithBinding(Container container, int min, int max, int value,
            IntConsumer changeListener) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, value);
        container.add(slider);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                changeListener.accept(slider.getValue());
            }
        });
        return slider;
    }
    // METHODS TO BIND ACTIONS TO COMPONENTS

    // METHODS CONCERNING GLYPHS
    public static Shape getShapeFromString(String s, float pointSize) {
        return getShapeFromString(s, DEFAULT_FONT.deriveFont(pointSize), DEFAULT_FONTRENDERCONTEXT);
    }

    public static Shape getShapeFromString(String s, Font font) {
        return getShapeFromString(s, font, DEFAULT_FONTRENDERCONTEXT);
    }

    public static Shape getShapeFromString(String s, float pointSize, FontRenderContext context) {
        return getShapeFromString(s, new JLabel().getFont().deriveFont(pointSize), context);
    }

    public static Shape getShapeFromString(String s, Font font, FontRenderContext context) {
        TextLayout layout = new TextLayout(s, font, context);
        return layout.getOutline(null);
    }
    // METHODS CONCERNING GLYPHS

    // METHODS CONCERNING FONTS
    public static Font getFontOrDefault(File file, float pointSize) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(pointSize);
        } catch (IOException | FontFormatException e) {
            System.out.println("DEFLUFELUEKLFEUE");
            return DEFAULT_FONT.deriveFont(pointSize);
        }
    }

    public static Font getRandomSystemFont(float pointSize) {
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        if (fonts.length > 0)
            return MyMath.randomFromArray(fonts).deriveFont(pointSize);
        else
            return DEFAULT_FONT.deriveFont(pointSize);
    }
    // METHODS CONCERNING FONTS

    // METHODS CONCERNING DRAWING
    public static void drawStringVariAnchor(Graphics2D g2d, int anchorX, int anchorY, boolean anchorLeft,
            boolean anchorBottom, String str) {
        int dx = anchorLeft ? 0 : g2d.getFontMetrics().stringWidth(str);
        int dy = anchorBottom ? 0 : g2d.getFontMetrics().getHeight();
        g2d.drawString(str, anchorX + dx, anchorY + dy);
    }

    @Deprecated
    // TODO: make this more like good plz like add variable justification and stuff
    /**
     * Make this implemented good please
     */
    public static void drawStringsLineByLineLeftJustified(Graphics2D g2d, int anchorX, int anchorY, boolean anchorLeft,
            boolean anchorBottom, String... strs) {
        int dx = 0, dy = 0;
        int strHeight = g2d.getFontMetrics().getHeight();
        if (!anchorLeft)
            for (String s : strs)
                dx = Math.max(dx, g2d.getFontMetrics().stringWidth(s));
        if (!anchorBottom)
            dy = strHeight;
        for (int i = 0; i < strs.length; ++i)
            g2d.drawString(strs[i], anchorX + dx, anchorY + dy + strHeight * i);
    }

    // TODO: Support cubics
    public static void drawShape(Graphics2D g2d, Shape shape, IntFunction<Color> colorOfEachSegment) {
        PathIterator pathIterator = shape.getPathIterator(null);
        int segmentIndex = 0;
        double lastX = 0, lastY = 0, currX = 0, currY = 0;
        double[] coords = new double[6];
        int segmentCode;
        while (!pathIterator.isDone()) {
            g2d.setColor(colorOfEachSegment.apply(segmentIndex++));
            segmentCode = pathIterator.currentSegment(coords);
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
//                g2d.drawLine((int) lastX, (int) lastY, (int) currX, (int) currY);
                break;
            case PathIterator.SEG_CUBICTO:
                System.out.println("CUBITO");
                currX = coords[4];
                currY = coords[5];
                g2d.drawLine((int) lastX, (int) lastY, (int) currX, (int) currY);
                break;
            default:
            case PathIterator.SEG_CLOSE:
                g2d.fillOval((int) (lastX - 2), (int) (lastY - 2), 4, 4);
                break;
            }
            lastX = currX;
            lastY = currY;
            pathIterator.next();
        }
    }
    // METHODS CONCERNING DRAWING
}
