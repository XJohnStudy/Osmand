package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.ACCURACY;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.GO_TO_NEXT_FIELD;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.RIGHT_HAND;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.USE_OSMAND_KEYBOARD;

public class CoordinateInputBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "CoordinateInputBottomSheetDialogFragment";

	private View mainView;
	private boolean useOsmandKeyboard;
	private boolean rightHand;
	private boolean goToNextField;
	private int accuracy;
	private CoordinateInputFormatChangeListener listener;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle args = getArguments();
			if (args != null) {
				useOsmandKeyboard = args.getBoolean(USE_OSMAND_KEYBOARD);
				rightHand = args.getBoolean(RIGHT_HAND);
				goToNextField = args.getBoolean(GO_TO_NEXT_FIELD);
				accuracy = args.getInt(ACCURACY);
			}
		} else {
			useOsmandKeyboard = savedInstanceState.getBoolean(USE_OSMAND_KEYBOARD);
			rightHand = savedInstanceState.getBoolean(RIGHT_HAND);
			goToNextField = savedInstanceState.getBoolean(GO_TO_NEXT_FIELD);
			accuracy = savedInstanceState.getInt(ACCURACY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_coordinate_input_options_bottom_sheet_helper, container);

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.coordinate_input_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((TextView) mainView.findViewById(R.id.coordinate_input_accuracy_descr)).setText(getString(R.string.coordinate_input_accuracy_description, accuracy));

		if (portrait) {
			mainView.findViewById(R.id.hand_row).setVisibility(View.GONE);
		}

		((CompoundButton) mainView.findViewById(R.id.go_to_next_field_switch)).setChecked(goToNextField);
		((ImageView) mainView.findViewById(R.id.go_to_next_field_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_keyboard));
		mainView.findViewById(R.id.go_to_next_field_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				goToNextField = !goToNextField;
				((CompoundButton) mainView.findViewById(R.id.go_to_next_field_switch)).setChecked(goToNextField);
				if (listener != null) {
					listener.onGoToNextFieldChanged(goToNextField);
				}
			}
		});

		if (accuracy == 4) {
			((RadioButton) mainView.findViewById(R.id.four_digits_radio_button)).setChecked(true);
		}
		((TextView) mainView.findViewById(R.id.four_digits_title)).setText(getString(R.string.coordinate_input_accuracy, 4));
		((TextView) mainView.findViewById(R.id.four_digits_description)).setText("00:00." + "5555");

		if (accuracy == 3) {
			((RadioButton) mainView.findViewById(R.id.three_digits_radio_button)).setChecked(true);
		}
		((TextView) mainView.findViewById(R.id.three_digits_title)).setText(getString(R.string.coordinate_input_accuracy, 3));
		((TextView) mainView.findViewById(R.id.three_digits_description)).setText("00:00." + "555");

		if (accuracy == 2) {
			((RadioButton) mainView.findViewById(R.id.two_digits_radio_button)).setChecked(true);
		}
		((TextView) mainView.findViewById(R.id.two_digits_title)).setText(getString(R.string.coordinate_input_accuracy, 2));
		((TextView) mainView.findViewById(R.id.two_digits_description)).setText("00:00." + "55");

		((CompoundButton) mainView.findViewById(R.id.use_system_keyboard_switch)).setChecked(!useOsmandKeyboard);
		((ImageView) mainView.findViewById(R.id.use_system_keyboard_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_keyboard));
		mainView.findViewById(R.id.use_system_keyboard_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				useOsmandKeyboard = !useOsmandKeyboard;
				((CompoundButton) mainView.findViewById(R.id.use_system_keyboard_switch)).setChecked(!useOsmandKeyboard);
				if (listener != null) {
					listener.onKeyboardChanged(useOsmandKeyboard);
				}
			}
		});
		highlightSelectedItem(true);

		View.OnClickListener accuracyChangedListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				highlightSelectedItem(false);
				switch (view.getId()) {
					case R.id.four_digits_row:
						accuracy = 4;
						break;
					case R.id.three_digits_row:
						accuracy = 3;
						break;
					case R.id.two_digits_row:
						accuracy = 2;
						break;
					default:
						throw new IllegalArgumentException("Unsupported accuracy");
				}
				highlightSelectedItem(true);
				if (listener != null) {
					listener.onAccuracyChanged(accuracy);
				}
			}
		};

		mainView.findViewById(R.id.four_digits_row).setOnClickListener(accuracyChangedListener);
		mainView.findViewById(R.id.three_digits_row).setOnClickListener(accuracyChangedListener);
		mainView.findViewById(R.id.two_digits_row).setOnClickListener(accuracyChangedListener);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.marker_coordinate_input_scroll_view);

		return mainView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(USE_OSMAND_KEYBOARD, useOsmandKeyboard);
		outState.putBoolean(RIGHT_HAND, rightHand);
		outState.putBoolean(GO_TO_NEXT_FIELD, goToNextField);
		outState.putInt(ACCURACY, accuracy);
		super.onSaveInstanceState(outState);
	}

	private void highlightSelectedItem(boolean check) {
		switch (accuracy) {
			case 4:
				((RadioButton) mainView.findViewById(R.id.four_digits_radio_button)).setChecked(check);
				break;
			case 3:
				((RadioButton) mainView.findViewById(R.id.three_digits_radio_button)).setChecked(check);
				break;
			case 2:
				((RadioButton) mainView.findViewById(R.id.two_digits_radio_button)).setChecked(check);
				break;
		}
	}

	interface CoordinateInputFormatChangeListener {

		void onKeyboardChanged(boolean useOsmandKeyboard);

		void onHandChanged(boolean rightHand);

		void onGoToNextFieldChanged(boolean goToNextField);

		void onAccuracyChanged(int accuracy);

	}
}
