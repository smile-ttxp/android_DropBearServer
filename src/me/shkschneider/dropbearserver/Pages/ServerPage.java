package me.shkschneider.dropbearserver.Pages;

import me.shkschneider.dropbearserver.MainActivity;
import me.shkschneider.dropbearserver.R;
import me.shkschneider.dropbearserver.SettingsHelper;
import me.shkschneider.dropbearserver.Tasks.Checker;
import me.shkschneider.dropbearserver.Tasks.CheckerCallback;
import me.shkschneider.dropbearserver.Tasks.DropbearInstaller;
import me.shkschneider.dropbearserver.Tasks.DropbearInstallerCallback;
import me.shkschneider.dropbearserver.Tasks.ServerStarter;
import me.shkschneider.dropbearserver.Tasks.ServerStarterCallback;
import me.shkschneider.dropbearserver.Tasks.ServerStopper;
import me.shkschneider.dropbearserver.Tasks.ServerStopperCallback;
import me.shkschneider.dropbearserver.Utils.RootUtils;
import me.shkschneider.dropbearserver.Utils.ServerUtils;
import me.shkschneider.dropbearserver.Utils.Utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ServerPage extends Activity implements OnClickListener, DropbearInstallerCallback<Boolean>, ServerStarterCallback<Boolean>, ServerStopperCallback<Boolean>, CheckerCallback<Boolean>
{
	private static final String TAG = "DropBearServer";

	private static final int STATUS_ERROR = 0x00;
	private static final int STATUS_STOPPED = 0x01;
	private static final int STATUS_STARTING = 0x02;
	private static final int STATUS_STARTED = 0x03;
	private static final int STATUS_STOPPING = 0x04;
	private static final int NOTIFICATION_ID = 1;

	private Context mContext;
	private View mView;
	private Integer mServerStatusCode;
	private Integer mListeningPort;
	private NotificationManager mNotificationManager;

	public static Integer mServerPid;

	private TextView mNetworkConnexion;
	private TextView mRootStatus;
	private LinearLayout mGetSuperuser;
	private LinearLayout mGetBusybox;
	private TextView mDropbearStatus;
	private LinearLayout mGetDropbear;
	private TextView mServerStatus;
	private LinearLayout mServerLaunch;
	private TextView mServerLaunchLabel;
	private TextView mServerLaunchPid;
	private LinearLayout mInfos;
	private TextView mInfosLabel;

	public ServerPage(Context context) {
		mContext = context;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(R.layout.server, null);
		mServerStatusCode = STATUS_ERROR;
		mListeningPort = SettingsHelper.LISTENING_PORT_DEFAULT;
		mServerPid = 0;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		// mNetworkConnexions
		mNetworkConnexion = (TextView) mView.findViewById(R.id.network_connexion);

		// mSuperuserStatus mGetSuperuser
		mRootStatus = (TextView) mView.findViewById(R.id.superuser_status);
		mGetSuperuser = (LinearLayout) mView.findViewById(R.id.get_superuser);
		mGetSuperuser.setOnClickListener(this);
		mGetBusybox = (LinearLayout) mView.findViewById(R.id.get_busybox);
		mGetBusybox.setOnClickListener(this);

		// mDropbearStatus mGetDropbear
		mDropbearStatus = (TextView) mView.findViewById(R.id.dropbear_status);
		mGetDropbear = (LinearLayout) mView.findViewById(R.id.get_dropbear);
		mGetDropbear.setOnClickListener(this);

		// mServerStatus mServerLaunch mServerLaunchLabel mServerLaunchPid
		mServerStatus = (TextView) mView.findViewById(R.id.server_status);
		mServerLaunch = (LinearLayout) mView.findViewById(R.id.server_launch);
		mServerLaunch.setOnClickListener(this);
		mServerLaunchLabel = (TextView) mView.findViewById(R.id.launch_label);
		mServerLaunchPid = (TextView) mView.findViewById(R.id.launch_pid);

		// mInfos mInfosLabel
		mInfos = (LinearLayout) mView.findViewById(R.id.infos);
		mInfosLabel = (TextView) mView.findViewById(R.id.infos_label);
	}

	public void updateAll() {
		updateNetworkStatus();
		updateRootStatus();
		updateDropbearStatus();
		updateServerStatusCode();
		updateServerStatus();
	}

	public void updateNetworkStatus() {
		if (ServerUtils.getLocalIpAddress() != null) {
			mNetworkConnexion.setText("OK");
			mNetworkConnexion.setTextColor(Color.GREEN);
		}
		else {
			mNetworkConnexion.setText("KO");
			mNetworkConnexion.setTextColor(Color.RED);
		}
	}

	public void updateRootStatus() {
		if (RootUtils.hasRootAccess == true) {
			if (RootUtils.hasBusybox == true) {
				mRootStatus.setText("OK");
				mRootStatus.setTextColor(Color.GREEN);
				mGetBusybox.setVisibility(View.GONE);
			}
			else {
				mRootStatus.setText("KO");
				mRootStatus.setTextColor(Color.RED);
				mGetBusybox.setVisibility(View.VISIBLE);
			}
			mGetSuperuser.setVisibility(View.GONE);
		}
		else {
			mRootStatus.setText("KO");
			mRootStatus.setTextColor(Color.RED);
			mGetSuperuser.setVisibility(View.VISIBLE);
		}
	}

	public void updateDropbearStatus() {
		if (RootUtils.hasRootAccess == true && RootUtils.hasBusybox == true) {
			if (RootUtils.hasDropbear == true) {
				mDropbearStatus.setText("OK");
				mDropbearStatus.setTextColor(Color.GREEN);
				mGetDropbear.setVisibility(View.GONE);
			}
			else {
				mDropbearStatus.setText("KO");
				mDropbearStatus.setTextColor(Color.RED);
				mGetDropbear.setVisibility(View.VISIBLE);
				mServerStatusCode = STATUS_ERROR;
			}
		}
		else {
			mDropbearStatus.setText("KO");
			mDropbearStatus.setTextColor(Color.RED);
			mGetDropbear.setVisibility(View.GONE);
			mServerStatusCode = STATUS_ERROR;
		}
	}

	public void updateServerStatusCode() {
		if (RootUtils.hasRootAccess == false || RootUtils.hasDropbear == false) {
			mServerStatusCode = STATUS_ERROR;
		}
		else {
			mServerPid = ServerUtils.getServerPidFromPs();
			Log.d(TAG, "ServerPage: updateServerStatusCode(): PID #" + mServerPid);
			if (mServerPid < 0) {
				mServerStatusCode = STATUS_ERROR;
			}
			else if (mServerPid == 0) {
				mServerStatusCode = STATUS_STOPPED;
			}
			else {
				mServerStatusCode = STATUS_STARTED;
			}
		}
	}

	public void updateServerStatus() {
		switch (mServerStatusCode) {
		case STATUS_STOPPED:
			mServerStatus.setText("STOPPED");
			mServerStatus.setTextColor(Color.RED);
			mServerLaunch.setVisibility(View.VISIBLE);
			mServerLaunchLabel.setText("START SERVER");
			mServerLaunchPid.setText("");
			mInfos.setVisibility(View.GONE);
			mInfosLabel.setText("");
			break;
		case STATUS_STARTING:
			mServerStatus.setText("STARTING");
			mServerStatus.setTextColor(Color.YELLOW);
			mServerLaunch.setVisibility(View.VISIBLE);
			mServerLaunchLabel.setText("STARTING...");
			mServerLaunchPid.setText("");
			mInfos.setVisibility(View.GONE);
			mInfosLabel.setText("");
			break;
		case STATUS_STARTED:
			mServerStatus.setText("STARTED");
			mServerStatus.setTextColor(Color.GREEN);
			mServerLaunch.setVisibility(View.VISIBLE);
			mServerLaunchLabel.setText("STOP SERVER");
			mServerLaunchPid.setText("PID " + mServerPid);
			mInfos.setVisibility(View.VISIBLE);

			String infos = "ssh ";
			if (SettingsHelper.getInstance(mContext).getCredentialsLogin() == true) {
				infos = infos.concat("root@");
			}
			infos = infos.concat(ServerUtils.getLocalIpAddress());
			if (mListeningPort != SettingsHelper.LISTENING_PORT_DEFAULT) {
				infos = infos.concat(" -p " + mListeningPort);
			}

			mInfosLabel.setText(infos);
			
			if (SettingsHelper.getInstance(mContext).getNotification() == true) {
				Notification notification = new Notification(R.drawable.ic_launcher, "DropBear Server is running", System.currentTimeMillis());
				Intent intent = new Intent(mContext, MainActivity.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
				notification.setLatestEventInfo(mContext, "DropBear Server", mInfosLabel.getText().toString(), pendingIntent);
				notification.flags |= Notification.FLAG_ONGOING_EVENT;
				mNotificationManager.notify(NOTIFICATION_ID, notification);
			}
			break;
		case STATUS_STOPPING:
			mServerStatus.setText("STOPPING");
			mServerStatus.setTextColor(Color.YELLOW);
			mServerLaunch.setVisibility(View.VISIBLE);
			mServerLaunchLabel.setText("STOPPING...");
			mServerLaunchPid.setText("");
			mInfos.setVisibility(View.GONE);
			mInfosLabel.setText("");
			break;
		case STATUS_ERROR:
			mServerStatus.setText("ERROR");
			mServerStatus.setTextColor(Color.RED);
			mServerLaunch.setVisibility(View.GONE);
			mServerLaunchLabel.setText("ERROR");
			mServerLaunchPid.setText("");
			mInfos.setVisibility(View.GONE);
			mInfosLabel.setText("");
			break;
		default:
			break;
		}
	}

	public View getView() {
		return mView;
	}

	public void onClick(View view) {
		if (view == mServerLaunch) {
			switch (mServerStatusCode) {
			case STATUS_STOPPED:
				mServerStatusCode = STATUS_STARTING;
				updateServerStatus();
				mListeningPort = SettingsHelper.getInstance(mContext).getListeningPort();
				// StartServer
				ServerStarter serverStarter = new ServerStarter(mContext, this);
				serverStarter.execute();
				break;
			case STATUS_STARTING:
				mServerStatusCode = STATUS_STARTED;
				break;
			case STATUS_STARTED:
				mServerStatusCode = STATUS_STOPPING;
				updateServerStatus();
				// StopServer
				ServerStopper serverStopper = new ServerStopper(mContext, this, mServerPid);
				serverStopper.execute();
				break;
			case STATUS_STOPPING:
				mServerStatusCode = STATUS_STOPPED;
				break;
			case STATUS_ERROR:
				mServerStatusCode = STATUS_ERROR;
				break;
			default:
				break;
			}
		}
		else if (view == mGetSuperuser) {
			try {
				Log.i(TAG, "ServerPage: onClick(): market://details?id=" + mContext.getResources().getString(R.string.superuser_package));
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mContext.getResources().getString(R.string.superuser_package))));
			}
			catch (ActivityNotFoundException e) {
				Utils.marketNotFound(mContext);
			}
			RootUtils.checkRootAccess();
			updateRootStatus();
		}
		else if (view == mGetBusybox) {
			try {
				Log.i(TAG, "ServerPage: onClick(): market://details?id=" + mContext.getResources().getString(R.string.busybox_package));
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mContext.getResources().getString(R.string.busybox_package))));
			}
			catch (ActivityNotFoundException e) {
				Utils.marketNotFound(mContext);
			}
			RootUtils.checkBusybox();
			updateRootStatus();
		}
		else if (view == mGetDropbear) {
			// DropbearInstaller
			DropbearInstaller dropbearInstaller = new DropbearInstaller(mContext, this);
			dropbearInstaller.execute();
		}
	}

	public void onDropbearInstallerComplete(Boolean result) {
		Log.i(TAG, "ServerPage: onDropbearInstallerComplete(" + result + ")");
		if (result == true) {
			RootUtils.checkDropbear(mContext);
			((MainActivity) mContext).updateSettings();
			((MainActivity) mContext).updateServer();
			((MainActivity) mContext).updateAbout();
			Toast.makeText(mContext, "DropBear successfully installed", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(mContext, "Use Settings > General > Complete removal to fix", Toast.LENGTH_LONG).show();
		}
	}

	public void onServerStarterComplete(Boolean result) {
		Log.i(TAG, "ServerPage: onStartServerComplete(" + result + ")");
		updateServerStatusCode();
		updateServerStatus(); // includes notification
	}

	public void onServerStopperComplete(Boolean result) {
		Log.i(TAG, "ServerPage: onStopServerComplete(" + result + ")");
		updateServerStatusCode();
		updateServerStatus();

		if (result == true && SettingsHelper.getInstance(mContext).getNotification() == true) {
			mNotificationManager.cancel(NOTIFICATION_ID);
		}
	}

	public void onCheckerComplete(Boolean result) {
		Log.i(TAG, "ServerPage: onCheckerComplete(" + result + ")");
		if (result == true) {
			updateAll();
		}
	}

	public void check() {
		// Checker
		Checker checker = new Checker(mContext, this);
		checker.execute();
	}
}
