package com.Orakatrap.rbx.UI.Elements.CustomDialogs;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.Orakatrap.rbx.R;
import com.Orakatrap.rbx.SettingsActivity;
import com.Orakatrap.rbx.Utility.FileTool;

import java.io.File;
import java.util.ArrayList;

public class AddFastFlagDialogFragment extends DialogFragment {

    private LinearLayout linear1, linear3, linear2, linear5;
    private EditText edittext1;
    private Button button1, button_ok, button_cancel;
    private TextView textview1;

    private final Intent JsonFilePicker = new Intent(Intent.ACTION_GET_CONTENT);

    private final ActivityResultLauncher<Intent> jsonFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> filePaths = new ArrayList<>();
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            filePaths.add(FileTool.convertUriToFilePath(requireContext(), uri));
                        }
                    } else if (data.getData() != null) {
                        filePaths.add(FileTool.convertUriToFilePath(requireContext(), data.getData()));
                    }

                    if (!filePaths.isEmpty()) {
                        edittext1.setText(FileTool.read(new File(filePaths.get(0))));
                    }
                }
            }
    );

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_fast_flag_dialog_fragment, container, false);
        initialize(savedInstanceState, view);
        initializeLogic();
        return view;
    }

    private void initialize(Bundle savedInstanceState, View view) {
        linear1 = view.findViewById(R.id.linear1);
        linear3 = view.findViewById(R.id.linear3);
        edittext1 = view.findViewById(R.id.edittext1);
        button1 = view.findViewById(R.id.button1);
        linear2 = view.findViewById(R.id.linear2);
        textview1 = view.findViewById(R.id.textview1);
        linear5 = view.findViewById(R.id.linear5);
        button_ok = view.findViewById(R.id.button_ok);
        button_cancel = view.findViewById(R.id.button_cancel);

        JsonFilePicker.setType("application/json");
        JsonFilePicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        button1.setOnClickListener(v -> jsonFilePickerLauncher.launch(JsonFilePicker));

        button_ok.setOnClickListener(v -> {
            if (requireActivity() instanceof SettingsActivity) {
                ((SettingsActivity) requireActivity())
                        .receiveFastFlagsListByDialog(edittext1.getText().toString());
            }
            dismiss();
        });

        button_cancel.setOnClickListener(v -> dismiss());
    }

    private void initializeLogic() {
        AstyleButton(button_ok);
        AstyleButton(button_cancel);
        setBackgroundDrawable(button_ok, 10, 2, 0xFF303030, 0xFF050505);
        setBackgroundDrawable(button1, 10, 2, 0xFF303030, 0xFF050505);
        setBackgroundDrawable(button_cancel, 10, 2, 0xFF303030, 0xFF050505);
        //setBackgroundDrawable(linear1, 10, 2, 0xFF303030, 0xFF131313);
        setBackgroundDrawable(edittext1, 10, 2, 0xFF303030, 0xFF202020);
    }

    private void AstyleButton(Button button) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(15);
        drawable.setStroke(2, Color.parseColor("#0C0F19")); // Stroke color
        drawable.setColor(Color.parseColor("#000000"));     // Fill color
        button.setBackground(drawable);
    }

    private void setBackgroundDrawable(View view, int radius, int stroke, int strokeColor, int fillColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setStroke(stroke, strokeColor);
        drawable.setColor(fillColor);
        view.setBackground(drawable);
    }
}
