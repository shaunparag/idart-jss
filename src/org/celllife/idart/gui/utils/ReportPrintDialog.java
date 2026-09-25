package org.celllife.idart.gui.utils;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

/**
 * The report viewer's print window: printer, pages and copies. It opens
 * straight away; the Windows print dialog Java uses can take several seconds
 * to open the first time in a session, and is still offered through More
 * settings.
 */
class ReportPrintDialog {

	enum Choice {
		PRINT, MORE_SETTINGS, CANCEL
	}

	/** The printer used last in this session, offered first next time. */
	private static String lastPrinter;

	private final Shell parent;

	private final int pageCount;

	private Choice choice = Choice.CANCEL;

	private PrintService printer;

	private int copies;

	private int firstPage;

	private int lastPage;

	ReportPrintDialog(Shell parent, int pageCount) {
		this.parent = parent;
		this.pageCount = pageCount;
	}

	/**
	 * Shows the window and waits until it is closed.
	 */
	Choice open() {
		final PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
		if (printers.length == 0) {
			MessageBox box = new MessageBox(parent, SWT.ICON_ERROR | SWT.OK);
			box.setText("Print");
			box.setMessage("There are no printers set up on this computer.");
			box.open();
			return Choice.CANCEL;
		}

		final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("Print");
		shell.setLayout(new GridLayout(2, false));

		new Label(shell, SWT.NONE).setText("Printer:");
		final Combo printerList = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
		printerList.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		for (PrintService p : printers) {
			printerList.add(p.getName());
		}
		printerList.select(indexOf(printers, preferredPrinter()));

		Label pagesLabel = new Label(shell, SWT.NONE);
		pagesLabel.setText("Pages:");
		pagesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		Composite pages = new Composite(shell, SWT.NONE);
		GridLayout pagesLayout = new GridLayout(4, false);
		pagesLayout.marginWidth = 0;
		pagesLayout.marginHeight = 0;
		pages.setLayout(pagesLayout);
		final Button all = new Button(pages, SWT.RADIO);
		all.setText(pageCount == 1 ? "All (1 page)" : "All (" + pageCount + " pages)");
		all.setSelection(true);
		all.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		final Button range = new Button(pages, SWT.RADIO);
		range.setText("From");
		range.setEnabled(pageCount > 1);
		final Spinner from = pageSpinner(pages, 1);
		new Label(pages, SWT.NONE).setText("to");
		final Spinner to = pageSpinner(pages, pageCount);

		new Label(shell, SWT.NONE).setText("Copies:");
		final Spinner copyCount = new Spinner(shell, SWT.BORDER);
		copyCount.setValues(1, 1, 99, 0, 1, 10);

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout buttonLayout = new GridLayout(3, false);
		buttonLayout.marginWidth = 0;
		buttons.setLayout(buttonLayout);
		Button more = new Button(buttons, SWT.PUSH);
		more.setText("More settings...");
		more.setToolTipText("Opens the Windows print window, for the tray, double-sided printing and other printer settings");
		more.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		Button print = new Button(buttons, SWT.PUSH);
		print.setText("Print");
		print.setLayoutData(buttonData(print));
		Button cancel = new Button(buttons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setLayoutData(buttonData(cancel));
		shell.setDefaultButton(print);

		SelectionAdapter rangeChosen = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				from.setEnabled(range.getSelection());
				to.setEnabled(range.getSelection());
			}
		};
		all.addSelectionListener(rangeChosen);
		range.addSelectionListener(rangeChosen);
		from.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (to.getSelection() < from.getSelection()) {
					to.setSelection(from.getSelection());
				}
			}
		});
		to.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (from.getSelection() > to.getSelection()) {
					from.setSelection(to.getSelection());
				}
			}
		});
		print.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				choice = Choice.PRINT;
				printer = printers[printerList.getSelectionIndex()];
				lastPrinter = printer.getName();
				copies = copyCount.getSelection();
				firstPage = range.getSelection() ? from.getSelection() - 1 : 0;
				lastPage = range.getSelection() ? to.getSelection() - 1 : pageCount - 1;
				shell.close();
			}
		});
		more.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				choice = Choice.MORE_SETTINGS;
				shell.close();
			}
		});
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});

		shell.pack();
		Point size = shell.getSize();
		Rectangle area = parent.getBounds();
		shell.setLocation(area.x + (area.width - size.x) / 2, area.y
				+ (area.height - size.y) / 3);
		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return choice;
	}

	PrintService getPrinter() {
		return printer;
	}

	int getCopies() {
		return copies;
	}

	/** First page to print, counting from 0. */
	int getFirstPage() {
		return firstPage;
	}

	/** Last page to print, counting from 0. */
	int getLastPage() {
		return lastPage;
	}

	private String preferredPrinter() {
		if (lastPrinter != null) {
			return lastPrinter;
		}
		PrintService standard = PrintServiceLookup.lookupDefaultPrintService();
		return standard == null ? null : standard.getName();
	}

	private static int indexOf(PrintService[] printers, String name) {
		for (int i = 0; i < printers.length; i++) {
			if (printers[i].getName().equals(name)) {
				return i;
			}
		}
		return 0;
	}

	private Spinner pageSpinner(Composite parent, int value) {
		Spinner spinner = new Spinner(parent, SWT.BORDER);
		spinner.setValues(value, 1, pageCount, 0, 1, 10);
		spinner.setEnabled(false);
		return spinner;
	}

	private static GridData buttonData(Button button) {
		GridData data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = Math.max(80, button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
		return data;
	}
}
