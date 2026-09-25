package org.celllife.idart.gui.utils;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.print.PrinterJob;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.standard.Media;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.print.JRPrinterAWT;

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
 * Printing from the report viewer. The viewer's own Print waits for the print
 * dialog on the viewer's thread, so the viewer showed "Not Responding" until
 * the dialog closed, and on the first print of a session the dialog took
 * several seconds to appear while Java set up the default printer.
 */
public final class ReportPrinting {

	private static final Logger log = Logger.getLogger(ReportPrinting.class);

	/**
	 * The job the printers were set up with at startup, kept so the default
	 * printer stays set up for the session.
	 */
	private static PrinterJob readyJob;

	/** What the startup set-up took, for the print trace. */
	private static volatile String startupTimes = "not finished";

	private ReportPrinting() {
	}

	/**
	 * Does the printer set-up a first print would do (finding the printers
	 * and preparing the default one), on a background thread while the user
	 * logs in. Windows only.
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
					Toolkit.getDefaultToolkit();
					GraphicsEnvironment.getLocalGraphicsEnvironment()
							.getDefaultScreenDevice().getDefaultConfiguration();
					long screen = System.currentTimeMillis();
					PrintService[] services = PrintServiceLookup
							.lookupPrintServices(null, null);
					for (PrintService service : services) {
						service.getAttributes();
						service.getSupportedAttributeValues(Media.class, null, null);
					}
					long found = System.currentTimeMillis();
					PrinterJob job = prepareJob();
					readyJob = job;
					startupTimes = "Java graphics ready in " + (screen - start) + " ms; "
							+ services.length + " printers found in " + (found - screen)
							+ " ms; default printer " + describe(job) + " ready in "
							+ (System.currentTimeMillis() - found) + " ms";
					log.info(startupTimes);
				} catch (Throwable t) {
					log.warn("Unable to set up the printers in advance; the first print may be slow.", t);
				}
			}
		};
		printers.setDaemon(true);
		printers.setPriority(Thread.MIN_PRIORITY);
		printers.start();
	}

	/**
	 * Replaces the viewer's Print, on the toolbar and in the File menu, with
	 * one that prints on its own thread, so the viewer keeps responding while
	 * the print dialog is open and while the pages are sent. Call before the
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
	 * The steps JasperReports takes before showing the print dialog.
	 */
	private static PrinterJob prepareJob() {
		PrinterJob job = PrinterJob.getPrinterJob();
		JRPrinterAWT.initPrinterJobFields(job);
		job.defaultPage();
		return job;
	}

	private static String describe(PrinterJob job) {
		PrintService service = job.getPrintService();
		return service == null ? "(none)" : "\"" + service.getName() + "\"";
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
			printing = true;
			setEnabled(false);
			final Display display = shell.getDisplay();
			shell.setCursor(display.getSystemCursor(SWT.CURSOR_APPSTARTING));

			Thread printThread = new Thread("print " + document.getName()) {
				@Override
				public void run() {
					Throwable failure = null;
					try {
						long start = System.currentTimeMillis();
						PrinterJob job = prepareJob();
						long ready = System.currentTimeMillis();
						boolean printed = JasperPrintManager.printReport(document, true);
						log.info("Print \"" + document.getName() + "\": printer "
								+ describe(job) + " ready in " + (ready - start) + " ms, "
								+ (printed ? "printed" : "cancelled") + " after "
								+ (System.currentTimeMillis() - ready) + " ms");
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
			if (!traced) {
				traced = true;
				new PrintTrace(printThread, display.getThread()).start();
			}
			printThread.start();
		}
	}

	/** Only the first print of a session is traced. */
	private static boolean traced;

	/**
	 * Temporary, to find what makes the first print slow: writes what the
	 * print and screen threads are doing, every 200 ms for up to 30 seconds
	 * after Print is pressed, to print-trace.txt. A thread is written out
	 * only when what it is doing changes.
	 */
	private static class PrintTrace extends Thread {

		private final Thread printThread;

		private final Thread screenThread;

		private final long start = System.currentTimeMillis();

		private final Map<Thread, String> last = new HashMap<Thread, String>();

		PrintTrace(Thread printThread, Thread screenThread) {
			super("print trace");
			this.printThread = printThread;
			this.screenThread = screenThread;
			setDaemon(true);
		}

		@Override
		public void run() {
			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileWriter("print-trace.txt"));
				out.println("iDART print trace, " + new Date());
				out.println("Java " + System.getProperty("java.version") + ", "
						+ System.getProperty("os.name") + " "
						+ System.getProperty("os.version"));
				out.println("At startup: " + startupTimes);
				out.println();
				long end = start + 30000;
				while (System.currentTimeMillis() < end) {
					sample(out);
					if (!printThread.isAlive() && printThread.getState() != State.NEW) {
						break;
					}
					Thread.sleep(200);
				}
				out.println("+" + (System.currentTimeMillis() - start) + " ms: "
						+ (printThread.isAlive() ? "trace stopped, still printing"
								: "printing finished"));
				log.info("Print trace written to print-trace.txt");
			} catch (Exception e) {
				log.warn("Unable to write the print trace", e);
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}

		private void sample(PrintWriter out) {
			long now = System.currentTimeMillis() - start;
			for (Map.Entry<Thread, StackTraceElement[]> entry : Thread
					.getAllStackTraces().entrySet()) {
				Thread thread = entry.getKey();
				String name = thread.getName();
				if (thread != printThread && thread != screenThread
						&& !name.startsWith("AWT") && !name.startsWith("Java2D")
						&& !name.startsWith("D3D") && !name.startsWith("Thread-")
						&& !name.equals("load printers")) {
					continue;
				}
				StringBuilder doing = new StringBuilder(thread.getState().toString());
				StackTraceElement[] stack = entry.getValue();
				for (int i = 0; i < stack.length && i < 30; i++) {
					doing.append("\n    at ").append(stack[i]);
				}
				if (!doing.toString().equals(last.get(thread))) {
					last.put(thread, doing.toString());
					out.println("+" + now + " ms " + name + " " + doing);
				}
			}
			out.flush();
		}
	}
}
