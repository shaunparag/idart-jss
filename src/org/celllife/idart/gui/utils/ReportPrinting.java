package org.celllife.idart.gui.utils;

import java.awt.Graphics;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
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
 * Printing from the report viewer. The viewer's own Print opens the Windows
 * print dialog through Java, which on some PCs takes several seconds to
 * appear the first time in a session while the viewer shows "Not
 * Responding". Print now opens iDART's own print window (ReportPrintDialog)
 * and prints on its own thread.
 */
public final class ReportPrinting {

	private static final Logger log = Logger.getLogger(ReportPrinting.class);

	private ReportPrinting() {
	}

	/**
	 * Finds the printers on a background thread while the user logs in, so
	 * the print window can list them straight away. Windows only.
	 */
	public static void loadPrintersInBackground() {
		if (!System.getProperty("os.name", "").toUpperCase().startsWith("WINDOWS")) {
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
	 * Prints pages firstPage to lastPage (counting from 0) on the printer
	 * without a print dialog, set up the same way as JasperReports' own
	 * printing (JRPrinterAWT): the paper is the report's page size, with no
	 * margins.
	 */
	static void printPages(JasperPrint document, PrintService printer, int copies,
			int firstPage, int lastPage) throws PrinterException, JRException {
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
		job.print();
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
			final ReportPrintDialog.Choice choice = dialog.open();
			if (choice == ReportPrintDialog.Choice.CANCEL || shell.isDisposed()) {
				return;
			}
			printing = true;
			setEnabled(false);
			final Display display = shell.getDisplay();
			shell.setCursor(display.getSystemCursor(SWT.CURSOR_APPSTARTING));

			new Thread("print " + document.getName()) {
				@Override
				public void run() {
					Throwable failure = null;
					try {
						if (choice == ReportPrintDialog.Choice.PRINT) {
							printPages(document, dialog.getPrinter(), dialog.getCopies(),
									dialog.getFirstPage(), dialog.getLastPage());
							log.info("Printed \"" + document.getName() + "\" on \""
									+ dialog.getPrinter().getName() + "\": pages "
									+ (dialog.getFirstPage() + 1) + "-"
									+ (dialog.getLastPage() + 1) + " of " + pageCount + ", "
									+ (dialog.getCopies() == 1 ? "1 copy"
											: dialog.getCopies() + " copies"));
						} else {
							boolean printed = JasperPrintManager.printReport(document, true);
							log.info((printed ? "Printed \"" : "Cancelled printing \"")
									+ document.getName() + "\" from the Windows print window");
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
			}.start();
		}
	}
}
