package net.dorokhov.pony.web.client.control;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import net.dorokhov.pony.web.client.util.ObjectUtils;

public class ArtworkLoader extends Composite {

	public enum State {
		EMPTY, LOADING, ERROR, LOADED
	}

	interface MyUiBinder extends UiBinder<Widget, ArtworkLoader> {}

	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

	@UiField
	Image emptyImage;

	@UiField
	Image loadingImage;

	@UiField
	Image errorImage;

	@UiField
	Image loadedImage;

	private String url;

	private State loadingState;

	public ArtworkLoader() {

		initWidget(uiBinder.createAndBindUi(this));

		setState(State.EMPTY);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String aUrl) {
		if (aUrl != null) {
			if (getState() == State.ERROR || !ObjectUtils.nullSafeEquals(url, aUrl)) {

				url = UriUtils.fromString(aUrl).asString();

				setState(State.LOADING);

				loadedImage.setUrl(aUrl);
			}
		} else {
			clear();
		}
	}

	public void setResource(ImageResource aResource) {
		if (aResource != null) {

			url = loadedImage.getUrl();

			loadedImage.setResource(aResource);

			setState(State.LOADED);

		} else {
			clear();
		}
	}

	public void clear() {

		url = null;

		setState(State.EMPTY);

		loadedImage.setUrl("");
	}

	public State getState() {
		return loadingState;
	}

	@UiHandler("loadedImage")
	void onImageLoaded(LoadEvent aEvent) {
		if (getState() == State.LOADING) {
			setState(State.LOADED);
		}
	}

	@UiHandler("loadedImage")
	void onImageError(ErrorEvent aEvent) {
		if (getState() == State.LOADING) {

			setState(State.ERROR);

			loadedImage.setUrl("");
		}
	}

	private void setState(State aLoadingState) {

		loadingState = aLoadingState;

		emptyImage.setVisible(getState() == State.EMPTY);
		loadingImage.setVisible(getState() == State.LOADING);
		errorImage.setVisible(getState() == State.ERROR);
		loadedImage.setVisible(getState() == State.LOADED);
	}

}