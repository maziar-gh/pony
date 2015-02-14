package net.dorokhov.pony.web.client.service;

import com.google.gwt.storage.client.Storage;
import net.dorokhov.pony.web.client.service.api.ApiService;
import net.dorokhov.pony.web.client.service.api.MethodCallbackAdapter;
import net.dorokhov.pony.web.client.service.api.RequestAdapter;
import net.dorokhov.pony.web.client.service.common.OperationCallback;
import net.dorokhov.pony.web.client.service.common.OperationRequest;
import net.dorokhov.pony.web.client.util.ErrorUtils;
import net.dorokhov.pony.web.shared.*;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AuthenticationManager {

	public interface InitializationHandler {
		public void onInitialization(UserDto aUser);
	}

	private static final String STORAGE_ACCESS_TOKEN_KEY = "AuthenticationStatusProvider.accessToken";
	private static final String STORAGE_ACCESS_TOKEN_EXPIRATION_KEY = "AuthenticationStatusProvider.accessTokenExpiration";

	private static final String STORAGE_REFRESH_TOKEN_KEY = "AuthenticationStatusProvider.refreshToken";

	private final Logger log = Logger.getLogger(getClass().getName());

	private final ApiService apiService;

	private Storage storage;

	private String accessToken;
	private String refreshToken;

	private Date accessTokenExpiration;

	private UserDto user;

	@Inject
	public AuthenticationManager(ApiService aApiService) {

		apiService = aApiService;

		storage = Storage.getLocalStorageIfSupported();
	}

	public boolean isAuthenticated() {
		return getUser() != null;
	}

	public UserDto getUser() {
		return user;
	}

	public String getAccessToken() {

		if (accessToken == null) {
			accessToken = fetchValue(STORAGE_ACCESS_TOKEN_KEY);
		}

		return accessToken;
	}

	private String getRefreshToken() {

		if (refreshToken == null) {
			refreshToken = fetchValue(STORAGE_REFRESH_TOKEN_KEY);
		}

		return refreshToken;
	}

	public void initialize(final InitializationHandler aCallback) {

		log.info("Initializing...");

		if (getAccessToken() != null) {
			updateUser(new OperationCallback<UserDto>() {
				@Override
				public void onSuccess(UserDto aUser) {
					aCallback.onInitialization(aUser);
				}

				@Override
				public void onError(List<ErrorDto> aErrors) {
					aCallback.onInitialization(null);
				}
			});
		} else {

			log.info("User is not authenticated.");

			aCallback.onInitialization(null);
		}
	}

	public OperationRequest updateUser(final OperationCallback<UserDto> aCallback) {

		log.info("Updating current user...");

		return new RequestAdapter(apiService.getCurrentUser(new MethodCallbackAdapter<>(new OperationCallback<UserDto>() {
			@Override
			public void onSuccess(UserDto aUser) {

				setUser(aUser);

				log.info("Current user is [" + aUser.getEmail() + "].");

				aCallback.onSuccess(aUser);
			}

			@Override
			public void onError(List<ErrorDto> aErrors) {

				if (ErrorUtils.getErrorByCode(ErrorCode.ACCESS_DENIED, aErrors) != null) {

					setAccessToken(null);
					setUser(null);

					log.info("User is not authenticated.");

				} else {
					log.info("Could not update authentication status.");
				}

				aCallback.onError(aErrors);
			}
		})));
	}

	public OperationRequest authenticate(final CredentialsDto aCredentials, final OperationCallback<UserDto> aCallback) {

		log.info("Authenticating user [" + aCredentials.getEmail() + "]...");

		return new RequestAdapter(apiService.authenticate(aCredentials, new MethodCallbackAdapter<>(new OperationCallback<AuthenticationDto>() {
			@Override
			public void onSuccess(AuthenticationDto aAuthentication) {

				setAccessToken(aAuthentication.getAccessToken());
				setAccessTokenExpiration(aAuthentication.getAccessTokenExpiration());

				setRefreshToken(aAuthentication.getAccessToken());

				setUser(aAuthentication.getUser());

				log.info("User [" + aAuthentication.getUser().getEmail() + "] has authenticated.");

				aCallback.onSuccess(aAuthentication.getUser());
			}

			@Override
			public void onError(List<ErrorDto> aErrors) {

				log.info("Authentication failed for user [" + aCredentials.getEmail() + "].");

				aCallback.onError(aErrors);
			}
		})));
	}

	public OperationRequest logout(final OperationCallback<UserDto> aCallback) {

		UserDto user = getUser();

		if (user != null) {
			log.info("Logging out user [" + user.getEmail() + "]...");
		} else {
			log.info("Logging out...");
		}

		OperationRequest request = new RequestAdapter(apiService.logout(new MethodCallbackAdapter<>(new OperationCallback<UserDto>() {
			@Override
			public void onSuccess(UserDto aUser) {

				log.info("User [" + aUser.getEmail() + "] has logged out.");

				aCallback.onSuccess(aUser);
			}

			@Override
			public void onError(List<ErrorDto> aErrors) {

				log.info("Logging out failed.");

				aCallback.onError(aErrors);
			}
		})));

		setAccessToken(null);
		setUser(null);

		return request;
	}

	private void setAccessToken(String aAccessToken) {

		accessToken = aAccessToken;

		storeValue(STORAGE_ACCESS_TOKEN_KEY, accessToken);
	}

	private Date getAccessTokenExpiration() {

		if (accessTokenExpiration == null) {

			String value = fetchValue(STORAGE_ACCESS_TOKEN_EXPIRATION_KEY);

			if (value != null) {
				accessTokenExpiration = new Date(Long.valueOf(value));
			}
		}

		return accessTokenExpiration;
	}

	private void setAccessTokenExpiration(Date aExpiration) {

		accessTokenExpiration = aExpiration;

		storeValue(STORAGE_ACCESS_TOKEN_EXPIRATION_KEY, accessTokenExpiration != null ? String.valueOf(accessTokenExpiration.getTime()) : null);
	}

	private void setRefreshToken(String aRefreshToken) {

		refreshToken = aRefreshToken;

		storeValue(STORAGE_REFRESH_TOKEN_KEY, refreshToken);
	}

	private void setUser(UserDto aUser) {
		user = aUser;
	}

	private String fetchValue(String aKey) {

		String value = null;

		if (storage != null) {
			value = storage.getItem(aKey);
		}

		return value;
	}

	private void storeValue(String aKey, String aValue) {
		if (storage != null) {
			if (aValue != null) {
				storage.setItem(aKey, aValue);
			} else {
				storage.removeItem(aKey);
			}
		}
	}

}
