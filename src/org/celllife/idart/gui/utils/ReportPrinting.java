package org.celllife.idart.gui.utils;

import java.awt.Graphics;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.reflect.Method;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.print.JRPrinterAWT;
import net.sf.jasperreports.engine.type.OrientationEnum;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.jasperassistant.designer.viewer.ViewerApp;
import com.jasperassistant.designer.viewer.actions.PrintAction;

/**
 * Printing from the report viewer. The viewer's own Print opened the Windows
 * print dialog through Java, which Windows opens behind the maximised viewer,
 * and the viewer waited for it, showing "Not Responding" until someone found
 * the dialog. Print now opens iDART's own print window (ReportPrintDialog)
 * and prints on its own thread; its More settings opens the Windows print
 * dialog and brings it to the front.
 */
public final class ReportPrinting {

	private static final Logger log = Logger.getLogger(ReportPrinting.class);

	private ReportPrinting() {
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toUpperCase().startsWith("WINDOWS");
	}

	/**
	 * Finds the printers on a background thread while the user logs in, so
	 * the print window can list them straight away. Windows only.
	 */
	public static void loadPrintersInBackground() {
		if (!isWindows()) {
			return;
		}
		Thread printers = new Thread("load printers") {
			@Override
			public void run() {
				try {
					long start = System.currentTimeMillis();
					PrintService[] services = PrintServiceLookup
							.lookupPrintServices(null, null);
					PrintService standard = PrintServiceLookup.lookupDefaultPrintService();
					log.info(services.length + " printers found in "
							+ (System.currentTimeMillis() - start) + " ms; default "
							+ (standard == null ? "(none)" : "\"" + standard.getName() + "\""));
				} catch (Throwable t) {
					log.warn("Unable to find the printers in advance.", t);
				}
			}
		};
		printers.setDaemon(true);
		printers.setPriority(Thread.MIN_PRIORITY);
		printers.start();
	}

	/**
	 * Replaces the viewer's Print, on the toolbar and in the File menu, with
	 * one that opens iDART's print window and prints on its own thread, so
	 * the viewer keeps responding while the pages are sent. Call before the
	 * viewer is created.
	 */
	public static void install(ViewerApp viewer) {
		PrintInBackground print = new PrintInBackground(viewer);
		replacePrint(viewer.getMenuBarManager(), print);
		replacePrint(viewer.getToolBarManager(), print);
	}

	private static void replacePrint(ContributionManager manager, PrintInBackground print) {
		IContributionItem[] items = manager.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof ContributionManager) {
				replacePrint((ContributionManager) items[i], print);
			} else if (items[i] instanceof ActionContributionItem) {
				IAction action = ((ActionContributionItem) items[i]).getAction();
				if (action instanceof PrintAction && action != print) {
					manager.remove(items[i]);
					manager.insert(i, new ActionContributionItem(print));
					((PrintAction) action).dispose();
				}
			}
		}
	}

	/**
	 * Prints pages firstPage to lastPage (counting from 0), set up the same
	 * way as JasperReports' own printing (JRPrinterAWT): the paper is the
	 * report's page size, with no margins. With chooseInWindows the Windows
	 * print dialog opens first, for the given printer.
	 *
	 * @return the printer printed on, or null if the Windows print dialog was
	 *         cancelled
	 */
	static PrintService printPages(JasperPrint document, PrintService printer,
			int copies, int firstPage, int lastPage, boolean chooseInWindows)
			throws PrinterException, JRException {
		PrinterJob job = PrinterJob.getPrinterJob();
		job.setPrintService(printer);
		PageFormat pageFormat = job.defaultPage();
		Paper paper = pageFormat.getPaper();
		job.setJobName("JasperReports - " + document.getName());
		int width = document.getPageWidth();
		int height = document.getPageHeight();
		if (document.getOrientationValue() == OrientationEnum.LANDSCAPE) {
			pageFormat.setOrientation(PageFormat.LANDSCAPE);
			paper.setSize(height, width);
			paper.setImageableArea(0, 0, height, width);
		} else {
			pageFormat.setOrientation(PageFormat.PORTRAIT);
			paper.setSize(width, height);
			paper.setImageableArea(0, 0, width, height);
		}
		pageFormat.setPaper(paper);
		Book book = new Book();
		book.append(new PageRange(document, firstPage), pageFormat, lastPage - firstPage + 1);
		job.setPageable(book);
		job.setCopies(copies);
		if (chooseInWindows && !job.printDialog()) {
			return null;
		}
		job.print();
		return job.getPrintService();
	}

	/** JasperReports' page printing, starting from a given page. */
	private static class PageRange extends JRPrinterAWT {

		private final int firstPage;

		PageRange(JasperPrint document, int firstPage) throws JRException {
			super(document);
			this.firstPage = firstPage;
		}

		@Override
		public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
				throws PrinterException {
			return super.print(graphics, pageFormat, firstPage + pageIndex);
		}
	}

	/**
	 * Brings the Windows print dialog in front of the viewer once it opens.
	 * Java opens it from another thread and without an owner, and Windows
	 * does not let such a window come to the front by itself; the viewer's
	 * thread, which has the front, can hand it over. Windows only.
	 */
	private static void bringPrintDialogForward(final Shell viewer, final Thread printThread) {
		if (!isWindows()) {
			return;
		}
		final Display display = viewer.getDisplay();
		final long start = System.currentTimeMillis();
		display.timerExec(100, new Runnable() {
			@Override
			public void run() {
				if (viewer.isDisposed() || !printThread.isAlive()
						|| System.currentTimeMillis() - start > 20000) {
					return;
				}
				try {
					long dialog = findPrintDialog(viewer.handle);
					if (dialog != 0) {
						boolean front = (Boolean) os("SetForegroundWindow", LONG, dialog);
						os("BringWindowToTop", LONG, dialog);
						log.info("Windows print dialog opened after "
								+ (System.currentTimeMillis() - start) + " ms"
								+ (front ? ", brought to the front" : ", could not bring it to the front"));
						return;
					}
				} catch (Exception e) {
					log.warn("Unable to bring the Windows print dialog to the front", e);
					return;
				}
				display.timerExec(100, this);
			}
		});
	}

	private static final Class<?>[] LONG = { long.class };

	private static final int GW_HWNDFIRST = 0;

	private static final int GW_HWNDNEXT = 2;

	/**
	 * The visible standard dialog ("#32770", the Windows print dialog) that
	 * belongs to iDART, or 0.
	 */
	private static long findPrintDialog(long viewer) throws Exception {
		int process = (Integer) os("GetCurrentProcessId", new Class<?>[0]);
		Class<?>[] getWindow = { long.class, int.class };
		long window = (Long) os("GetWindow", getWindow, viewer, GW_HWNDFIRST);
		while (window != 0) {
			int[] owner = new int[1];
			os("GetWindowThreadProcessId", new Class<?>[] { long.class, int[].class }, window, owner);
			if (owner[0] == process && (Boolean) os("IsWindowVisible", LONG, window)) {
				char[] name = new char[16];
				int length = (Integer) os("GetClassName",
						new Class<?>[] { long.class, char[].class, int.class }, window, name,
						name.length);
				if ("#32770".equals(new String(name, 0, length))) {
					return window;
				}
			}
			window = (Long) os("GetWindow", getWindow, window, GW_HWNDNEXT);
		}
		return 0;
	}

	/**
	 * Calls a Windows function through SWT's Windows build, which this source
	 * is not compiled against on other systems.
	 */
	private static Object os(String function, Class<?>[] types, Object... args)
			throws Exception {
		Method method = Class.forName("org.eclipse.swt.internal.win32.OS").getMethod(
				function, types);
		return method.invoke(null, args);
	}

	private static class PrintInBackground extends PrintAction {

		private final ViewerApp viewer;

		private boolean printing;

		PrintInBackground(ViewerApp viewer) {
			super(viewer.getReportViewer());
			this.viewer = viewer;
		}

		@Override
		protected boolean calculateEnabled() {
			return !printing && super.calculateEnabled();
		}

		@Override
		public void run() {
			final JasperPrint document = getReportViewer().getDocument();
			final Shell shell = viewer.getShell();
			if (printing || document == null || shell == null) {
				return;
			}
			final int pageCount = document.getPages().size();
			final ReportPrintDialog dialog = new ReportPrintDialog(shell, pageCount);
			ReportPrintDialog.Choice choice = dialog.open();
			if (choice == ReportPrintDialog.Choice.CANCEL || shell.isDisposed()) {
				return;
			}
			// More settings: all pages, chosen in the Windows print dialog
			final boolean inWindows = choice == ReportPrintDialog.Choice.MORE_SETTINGS;
			final int firstPage = inWindows ? 0 : dialog.getFirstPage();
			final int lastPage = inWindows ? pageCount - 1 : dialog.getLastPage();
			printing = true;
			setEnabled(false);
			final Display display = shell.getDisplay();
			shell.setCursor(display.getSystemCursor(SWT.CURSOR_APPSTARTING));

			Thread printThread = new Thread("print " + document.getName()) {
				@Override
				public void run() {
					Throwable failure = null;
					try {
						PrintService printer = printPages(document, dialog.getPrinter(),
								dialog.getCopies(), firstPage, lastPage, inWindows);
						if (printer == null) {
							log.info("Cancelled printing \"" + document.getName()
									+ "\" in the Windows print dialog");
						} else {
							ReportPrintDialog.rememberPrinter(printer.getName());
							log.info("Printed \"" + document.getName() + "\" on \""
									+ printer.getName() + "\""
									+ (inWindows ? " from the Windows print dialog"
											: ": pages " + (firstPage + 1) + "-"
													+ (lastPage + 1) + " of " + pageCount
													+ ", " + (dialog.getCopies() == 1 ? "1 copy"
															: dialog.getCopies() + " copies")));
						}
					} catch (Throwable t) {
						log.error("Unable to print \"" + document.getName() + "\"", t);
						failure = t;
					}
					final Throwable error = failure;
					if (display.isDisposed()) {
						return;
					}
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							printing = false;
							if (shell.isDisposed()) {
								return;
							}
							shell.setCursor(null);
							setEnabled(calculateEnabled());
							if (error != null) {
								MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
								box.setText("Printing Error");
								box.setMessage("Failed to print the document: "
										+ error.getMessage());
								box.open();
							}
						}
					});
				}
			};
			printThread.start();
			if (inWindows) {
				bringPrintDialogForward(shell, printThread);
			}
		}
	}
}
