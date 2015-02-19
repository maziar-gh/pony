package net.dorokhov.pony.web.client.mvp.library.artist;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;
import net.dorokhov.pony.web.client.mvp.common.LoadingState;
import net.dorokhov.pony.web.shared.ArtistDto;
import org.gwtbootstrap3.client.ui.LinkedGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtistsView extends ViewWithUiHandlers<ArtistsUiHandlers> implements ArtistsPresenter.MyView {

	interface MyUiBinder extends UiBinder<Widget, ArtistsView> {}

	private static final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

	private final List<ArtistLinkView> viewCache = new ArrayList<>();
	
	private final Map<ArtistDto, ArtistLinkView> artistToView = new HashMap<>();

	private final SingleSelectionModel<ArtistDto> selectionModel = new SingleSelectionModel<>();
	
	@UiField
	LinkedGroup artistList;
	
	@UiField
	Label loadingLabel;
	
	@UiField
	Label errorLabel;

	private List<ArtistDto> artists;

	private LoadingState loadingState;

	public ArtistsView() {
		
		initWidget(uiBinder.createAndBindUi(this));
		
		selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				
				updateArtistViews();

				ArtistDto artist = selectionModel.getSelectedObject();

				getUiHandlers().onArtistSelection(artist);
			}
		});

		for (int i = 0; i < 150; i++) {
			viewCache.add(createArtistView());
		}
		
		setLoadingState(LoadingState.LOADING);
	}

	@Override
	public List<ArtistDto> getArtists() {
		
		if (artists == null) {
			artists = new ArrayList<>();
		}
		
		return artists;
	}

	@Override
	public void setArtists(List<ArtistDto> aArtists) {

		artists = aArtists;

		updateArtists();
	}

	@Override
	public ArtistDto getSelectedArtist() {
		return selectionModel.getSelectedObject();
	}

	@Override
	public void setSelectedArtist(ArtistDto aArtist, boolean aShouldScroll) {

		selectionModel.setSelected(aArtist, true);

		if (aShouldScroll && artists != null) {

			final ArtistLinkView artistView = artistToView.get(aArtist);

			Scheduler.get().scheduleFinally(new Command() {
				@Override
				public void execute() {
					artistView.getElement().scrollIntoView();
				}
			});
		}
	}

	@Override
	public LoadingState getLoadingState() {
		return loadingState;
	}

	@Override
	public void setLoadingState(LoadingState aLoadingState) {

		loadingState = aLoadingState;
		
		updateLoadingState();
	}

	private void updateArtists() {

		while (artistList.getWidgetCount() > getArtists().size()) {

			int i = artistList.getWidgetCount() - 1;

			ArtistLinkView artistView = (ArtistLinkView) artistList.getWidget(i);
			
			artistList.remove(i);
			
			artistView.setArtist(null);

			viewCache.add(artistView);
		}
		
		artistToView.clear();

		for (int i = 0; i < getArtists().size(); i++) {

			ArtistDto artist = getArtists().get(i);

			ArtistLinkView artistView;
			if (i < artistList.getWidgetCount()) {
				artistView = (ArtistLinkView) artistList.getWidget(i);
			} else {

				artistView = viewCache.size() > 0 ? viewCache.remove(0) : null;

				if (artistView == null) {
					artistView = createArtistView();
				}

				artistList.add(artistView);
			}

			artistView.setArtist(artist);

			artistToView.put(artist, artistView);
		}

		updateArtistViews();
	}

	private void updateArtistViews() {
		for (Map.Entry<ArtistDto, ArtistLinkView> entry : artistToView.entrySet()) {
			entry.getValue().setActive(selectionModel.isSelected(entry.getKey()));
		}
	}

	private void updateLoadingState() {
		loadingLabel.setVisible(getLoadingState() == LoadingState.LOADING);
		errorLabel.setVisible(getLoadingState() == LoadingState.ERROR);
		artistList.setVisible(getLoadingState() == LoadingState.LOADED);
	}

	private ArtistLinkView createArtistView() {

		final ArtistLinkView artistView = new ArtistLinkView();

		artistView.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				selectionModel.setSelected(artistView.getArtist(), true);
			}
		});

		return artistView;
	}

}
