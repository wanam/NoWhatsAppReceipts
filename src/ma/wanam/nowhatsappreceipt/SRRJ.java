package ma.wanam.nowhatsappreceipt;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SRRJ implements IXposedHookLoadPackage {

	private static final String COM_WHATSAPP = "com.whatsapp";
	private static final String TAG = "NoWhatsAppReceipts: ";
	private static final String MODULE = "ma.wanam.nowhatsappreceipt";
	private static ArrayList<Object> srrjs = new ArrayList<Object>();
	private static ArrayList<Object> srrjsTobeRemoved = new ArrayList<Object>();
	private static Context context = null;
	private static char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static char[] alphabetSub = " abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static ClassLoader classLoader = null;
	private static String versionCode;
	private static String moduleVersionCode;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(COM_WHATSAPP)) {

			SRRJ.classLoader = lpparam.classLoader;

			try {
				if (context == null) {
					Object activityThread = XposedHelpers.callStaticMethod(
							XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
					context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
				}

				versionCode = context.getPackageManager().getPackageInfo(COM_WHATSAPP, 0).versionName;
				moduleVersionCode = context.getPackageManager().getPackageInfo(MODULE, 0).versionName;
			} catch (Throwable e) {
				XposedBridge.log(e);
			}

			try {
				XposedHelpers.findAndHookMethod(COM_WHATSAPP + ".jobqueue.job.SendReadReceiptJob", lpparam.classLoader,
						"b", new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								srrjs.add(param.thisObject);
								param.setResult(null);
								return;
							}
						});

			} catch (Throwable e) {
				XposedBridge.log(e);
			}

			try {
				XposedHelpers.findAndHookMethod(COM_WHATSAPP + ".jobqueue.job.SendE2EMessageJob", lpparam.classLoader,
						"b", new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								String jid = (String) XposedHelpers.getObjectField(param.thisObject, "jid");

								for (Object srrj : srrjs) {
									if (jid.equals(XposedHelpers.getObjectField(srrj, "jid"))) {
										Member b = srrj.getClass().getMethod("b");
										XposedBridge.invokeOriginalMethod(b, srrj, null);
										srrjsTobeRemoved.add(srrj);
									}
								}

								if (!srrjsTobeRemoved.isEmpty()) {
									srrjs.removeAll(srrjsTobeRemoved);
									srrjsTobeRemoved.clear();
								}

							}
						});

			} catch (Throwable e) {
				XposedBridge.log(e);
			}

			new BFAsync().execute();

		}

		if (lpparam.packageName.equals("ma.wanam.nowhatsappreceipt")) {
			try {
				XposedHelpers.findAndHookMethod("ma.wanam.nowhatsappreceipt.XChecker", lpparam.classLoader,
						"isEnabled", XC_MethodReplacement.returnConstant(Boolean.TRUE));
			} catch (Throwable t) {
				XposedBridge.log(t);
			}
		}

	}

	private static class BFAsync extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {

			boolean found = false;
			for (char a1 : alphabet) {
				for (char a2 : alphabetSub) {
					found = findAndHookWhatsApp(a1, a2);
					if (found) {
						return true;
					}
				}
			}

			return false;
		}

		/**
		 * @param a1
		 * @param a2
		 * @return true if a hook was found
		 */
		private boolean findAndHookWhatsApp(char a1, char a2) {
			Class<?> classObj;
			String classStr;

			try {
				classStr = new StringBuffer().append(COM_WHATSAPP).append(".messaging.").append(a1).append(a2)
						.toString().trim();
				classObj = XposedHelpers.findClass(classStr, classLoader);

			} catch (Throwable e1) {
				return false;
			}

			try {
				if (XposedHelpers.findFirstFieldByExactType(classObj, LinkedList.class).getName().equals("b")) {
					XposedHelpers.findMethodExact(classObj, "a", Message.class);
					try {

						// xmpp/writer/write/composing
						XposedHelpers.findAndHookMethod(classStr + "$c", classLoader, "a", String.class, int.class,
								XC_MethodReplacement.DO_NOTHING);

						// xmpp/writer/write/inactive
						XposedHelpers.findAndHookMethod(classStr + "$c", classLoader, "c",
								XC_MethodReplacement.DO_NOTHING);

						// xmpp/writer/write/active
						XposedHelpers.findAndHookMethod(classStr + "$c", classLoader, "d",
								XC_MethodReplacement.DO_NOTHING);

						XposedBridge.log(TAG + "Successfully loaded WhatsApp " + versionCode + " with module version "
								+ moduleVersionCode);
						return true;
					} catch (Throwable e) {
						XposedBridge.log(e);
					}

				}

			} catch (Throwable e) {
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean found) {

			if (!found) {
				XposedBridge.log(TAG + "WhatsApp " + versionCode + " is incompatible with module version "
						+ moduleVersionCode);
			}
		}

	}
}