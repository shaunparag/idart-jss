package org.celllife.idart.gui.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/**
 * Draws the drop-down arrow on every CCombo. On Windows, SWT draws a CCombo's
 * arrow button with the scroll bar theme, which Windows 11 leaves as an empty
 * box, so this paints a chevron over it like a native drop-down's.
 */
public final class CComboArrows {

	private static final String INSTALLED = CComboArrows.class.getName();

	private static final PaintListener PAINTER = new PaintListener() {
		@Override
		public void paintControl(PaintEvent e) {
			paint((Button) e.widget, e.gc);
		}
	};

	private static final Listener REDRAW = new Listener() {
		@Override
		public void handleEvent(Event e) {
			((Control) e.widget).redraw();
		}
	};

	private CComboArrows() {
	}

	/**
	 * Draws the arrow on every CCombo created from now on. A CCombo lays out
	 * its arrow button as soon as it is created, which is when this finds it.
	 */
	public static void install(Display display) {
		display.addFilter(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event e) {
				if (isComboArrow(e.widget) && e.widget.getData(INSTALLED) == null) {
					Button arrow = (Button) e.widget;
					arrow.setData(INSTALLED, Boolean.TRUE);
					arrow.addPaintListener(PAINTER);
					// Windows repaints a button as it is pressed and released
					// without a paint event, which would wipe the chevron
					arrow.addListener(SWT.MouseDown, REDRAW);
					arrow.addListener(SWT.MouseUp, REDRAW);
					arrow.redraw();
				}
			}
		});
	}

	private static boolean isComboArrow(Widget widget) {
		return widget instanceof Button && (widget.getStyle() & SWT.ARROW) != 0
				&& ((Button) widget).getParent() instanceof CCombo;
	}

	private static void paint(Button arrow, GC gc) {
		Point size = arrow.getSize();
		gc.setBackground(fieldBackground(arrow));
		gc.fillRectangle(0, 0, size.x, size.y);

		int width = Math.max(8, Math.min(size.x, size.y) * 9 / 20);
		width -= width % 2;
		int height = width / 2;
		int left = (size.x - width) / 2;
		int top = (size.y - height) / 2;
		gc.setAntialias(SWT.ON);
		gc.setForeground(arrow.getDisplay().getSystemColor(arrow.isEnabled()
				? SWT.COLOR_WIDGET_FOREGROUND : SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawPolyline(new int[] { left, top, left + height, top + height,
				left + width, top });
	}

	/** The background of the combo's text, so the arrow looks part of it. */
	private static Color fieldBackground(Button arrow) {
		for (Control sibling : arrow.getParent().getChildren()) {
			if (sibling instanceof Text) {
				return sibling.getBackground();
			}
		}
		return arrow.getBackground();
	}
}
