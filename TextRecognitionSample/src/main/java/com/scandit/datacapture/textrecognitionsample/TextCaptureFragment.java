/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scandit.datacapture.textrecognitionsample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.scandit.datacapture.core.common.feedback.Feedback;
import com.scandit.datacapture.core.common.geometry.FloatWithUnit;
import com.scandit.datacapture.core.common.geometry.MeasureUnit;
import com.scandit.datacapture.core.common.geometry.SizeWithUnit;
import com.scandit.datacapture.core.data.FrameData;
import com.scandit.datacapture.core.source.FrameSourceState;
import com.scandit.datacapture.core.ui.DataCaptureView;
import com.scandit.datacapture.core.ui.viewfinder.RectangularViewfinder;
import com.scandit.datacapture.text.capture.TextCapture;
import com.scandit.datacapture.text.feedback.TextCaptureFeedback;
import com.scandit.datacapture.text.capture.TextCaptureListener;
import com.scandit.datacapture.text.capture.TextCaptureSession;
import com.scandit.datacapture.text.data.CapturedText;
import com.scandit.datacapture.text.ui.TextCaptureOverlay;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * A fragment that performs text recognition.
 */
public class TextCaptureFragment extends CameraPermissionFragment
        implements TextCaptureListener, TextCaptureResultDialogFragment.Callbacks {
    private static final String RESULT_FRAGMENT_TAG = "result_fragment";

    private DataCaptureManager dataCaptureManager;
    private DataCaptureView view;
    private Feedback feedback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @NotNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_text_capture, container, false);

        dataCaptureManager = DataCaptureManager.getInstance(requireContext());

        /*
         * Create a new DataCaptureView and fill the screen with it. DataCaptureView will show
         * the camera preview on the screen. Pass your DataCaptureContext to the view's
         * constructor.
         */
        view = DataCaptureView.newInstance(
                requireContext(),
                dataCaptureManager.getDataCaptureContext()
        );
        root.addView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        /*
         * Add TextCaptureOverlay with RectangularViewfinder to DataCaptureView. This will visualize
         * the part of the frame currently taken into account for the text recognition.
         */
        RectangularViewfinder viewfinder = new RectangularViewfinder();
        viewfinder.setSize(
                new SizeWithUnit(
                        new FloatWithUnit(1f, MeasureUnit.FRACTION),
                        new FloatWithUnit(0.4f, MeasureUnit.FRACTION)
                )
        );

        TextCaptureOverlay overlay =
                TextCaptureOverlay.newInstance(dataCaptureManager.getTextCapture(), view);
        overlay.setViewfinder(viewfinder);

        /*
         * Since we additionally validate IBANs in this sample, we don't want to emit feedback
         * every time something is recognized, but every time the recognized text additionally
         * passes our validation. Therefore we disable the default feedback for TextCapture
         * and create a new one, which we will invoke manually.
         */
        TextCaptureFeedback noFeedback = TextCaptureFeedback.defaultFeedback();
        noFeedback.setSuccess(new Feedback());
        dataCaptureManager.getTextCapture().setFeedback(noFeedback);

        feedback = Feedback.defaultFeedback();

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_menu, menu);
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         * Check for camera permission and request it, if it hasn't yet been granted.
         * Once we have the permission the onCameraPermissionGranted() method will be called.
         */
        requestCameraPermission();
    }

    @Override
    public void onCameraPermissionGranted() {
        /*
         * Start listening on TextCapture events
         */
        dataCaptureManager.getTextCapture().addListener(this);

        /*
         * Switch the camera on. The camera frames will be sent to TextCapture for processing.
         * Additionally the preview will appear on the screen. The camera is started asynchronously,
         * and you may notice a small delay before the preview appears.
         */
        dataCaptureManager.getCamera().switchToDesiredState(FrameSourceState.ON);
    }

    @Override
    public void onPause() {
        super.onPause();

        /*
         * Switch the camera off to stop streaming frames. The camera is stopped asynchronously.
         */
        dataCaptureManager.getCamera().switchToDesiredState(FrameSourceState.OFF);
        dataCaptureManager.getTextCapture().removeListener(this);
    }

    @Override
    public void onTextCaptured(
            @NotNull TextCapture mode,
            @NotNull TextCaptureSession session,
            @NotNull FrameData data
    ) {
        /*
         * Collect the captured texts.
         */
        List<CapturedText> texts = session.getNewlyCapturedTexts();

        if (!texts.isEmpty()) {
            /*
             * Here you may perform additional validation or processing of the captured texts.
             * For example, if we recognize IBANs, we will additionally validate them.
             */
            final CapturedText text = texts.get(0);

            boolean isValidText = true;

            if (dataCaptureManager.isCapturingIbans()) {
                isValidText = new IbanValidator(text.getValue()).isValid();
            }

            if (isValidText) {
                /*
                 * This callback may be executed on an arbitrary thread. We post to switch back
                 * to the main thread.
                 */
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        showResult(text.getValue());
                    }
                });
            }
        }
    }

    @Override
    public void onObservationStarted(@NotNull TextCapture mode) {
        // In this sample we are not interested in this callback.
    }

    @Override
    public void onObservationStopped(@NotNull TextCapture mode) {
        // In this sample we are not interested in this callback.
    }

    private void showResult(String text) {
        if (isResumed()) {
            /*
             * Emit feedback signalling that text was recognized.
             */
            feedback.emit();

            /*
             * Show the result fragment only if we are not displaying one at the moment.
             */
            if (getChildFragmentManager().findFragmentByTag(RESULT_FRAGMENT_TAG) == null) {
                TextCaptureResultDialogFragment
                        .newInstance(text)
                        .show(getChildFragmentManager(), RESULT_FRAGMENT_TAG);

                /*
                 * Don't capture unnecessarily when the result is displayed.
                 */
                dataCaptureManager.getTextCapture().setEnabled(false);
            }
        }
    }

    @Override
    public void onResultDismissed() {
        /*
         * Enable capture again, after the result dialog is dismissed.
         */
        dataCaptureManager.getTextCapture().setEnabled(true);
    }
}
