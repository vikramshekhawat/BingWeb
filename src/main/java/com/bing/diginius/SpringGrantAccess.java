package com.bing.diginius;

import java.net.URL;
import java.rmi.RemoteException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.bing.diginius.daoImpl.SaveToMySqlDaoImpl;
import com.microsoft.bingads.AuthorizationData;
import com.microsoft.bingads.OAuthTokens;
import com.microsoft.bingads.OAuthWebAuthCodeGrant;
import com.microsoft.bingads.ServiceClient;
import com.microsoft.bingads.v11.customermanagement.Account;
import com.microsoft.bingads.v11.customermanagement.ArrayOfAccount;
import com.microsoft.bingads.v11.customermanagement.ArrayOfPredicate;
import com.microsoft.bingads.v11.customermanagement.GetUserRequest;
import com.microsoft.bingads.v11.customermanagement.ICustomerManagementService;
import com.microsoft.bingads.v11.customermanagement.Paging;
import com.microsoft.bingads.v11.customermanagement.Predicate;
import com.microsoft.bingads.v11.customermanagement.PredicateOperator;
import com.microsoft.bingads.v11.customermanagement.SearchAccountsRequest;
import com.microsoft.bingads.v11.customermanagement.User;

@Controller
@RequestMapping("/")
public class SpringGrantAccess {

	private static final String ClientId = "19fe1177-3fc9-41d5-b0d1-b51298fa9805";
	private static final String ClientSecret = "jh6LdOdnU9tSSBVkrRejV0w";
	private static final String RedirectionUri = "http://localhost:8080/BingWeb/bing/";
	private static final String BingState = "bing_client_state";
	private static final String DeveloperToken = "1239G0A646876760";
	static ServiceClient<ICustomerManagementService> CustomerService;

	private OAuthWebAuthCodeGrant oAuthWebAuthCodeGrant = null;

	@Autowired
	private SaveToMySqlDaoImpl saveToMySqlDaoImpl;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public void goToAuthorization(HttpServletRequest request, HttpServletResponse response) {
		try {
			oAuthWebAuthCodeGrant = new OAuthWebAuthCodeGrant(ClientId, ClientSecret, new URL(RedirectionUri));
			oAuthWebAuthCodeGrant.setState(BingState);
			URL authorizationEndpoint = oAuthWebAuthCodeGrant.getAuthorizationEndpoint();
			response.sendRedirect(authorizationEndpoint.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/bing/", method = RequestMethod.GET)
	public void getToken(HttpServletRequest request, HttpServletResponse response) {
		try {
			if (request.getParameter("code") != null) {
				if (oAuthWebAuthCodeGrant.getState() != BingState)
					throw new Exception("The OAuth response state does not match the client request state.");
				OAuthTokens oauthTokens = oAuthWebAuthCodeGrant.requestAccessAndRefreshTokens(
						new URL(request.getRequestURL() + "?" + request.getQueryString()));
				/*
				 * System.out.println(oauthTokens.getRefreshToken());
				 * System.out.println(oauthTokens.getAccessToken());
				 */
				AuthorizationData authorizationData = new AuthorizationData();
				authorizationData.setDeveloperToken(DeveloperToken);
				authorizationData.setAuthentication(oAuthWebAuthCodeGrant);

				CustomerService = new ServiceClient<ICustomerManagementService>(authorizationData,
						ICustomerManagementService.class);
				User user = getUser(null);
				ArrayOfAccount accounts = searchAccountsByUserId(user.getId());
				System.out.println("The user can access the following Bing Ads accounts: \n");
				saveToMysql(accounts, oauthTokens.getRefreshToken());

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	private User getUser(java.lang.Long userId) throws RemoteException, Exception {
		GetUserRequest request = new GetUserRequest();

		return CustomerService.getService().getUser(request).getUser();
	}

	private ArrayOfAccount searchAccountsByUserId(java.lang.Long userId) {
		ArrayOfAccount s = null;
		SearchAccountsRequest searchAccountsRequest = null;
		try {
			ArrayOfPredicate predicates = new ArrayOfPredicate();
			Predicate predicate = new Predicate();
			predicate.setField("UserId");
			predicate.setOperator(PredicateOperator.EQUALS);
			predicate.setValue("" + userId);
			predicates.getPredicates().add(predicate);

			Paging paging = new Paging();
			paging.setIndex(0);
			paging.setSize(20);

			searchAccountsRequest = new SearchAccountsRequest();
			searchAccountsRequest.setPredicates(predicates);
			searchAccountsRequest.setPageInfo(paging);

			s = CustomerService.getService().searchAccounts(searchAccountsRequest).getAccounts();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return s;

	}

	private void saveToMysql(ArrayOfAccount accounts, String refreshToken) throws RemoteException, Exception {
		for (Account account : accounts.getAccounts()) {
			System.out.printf("AccountId: %d\n", account.getId());
			System.out.printf("CustomerId: %d\n", account.getParentCustomerId());
			System.out.println("CustomerName:" + account.getName());
			System.out.println("CustomerBingId:" + account.getNumber());
			System.out.println("CustomerBingPrimaryId:" + account.getPrimaryUserId());

			System.out.println();

			saveToMySqlDaoImpl.saveAllInfoToMySql(account.getId(), account.getParentCustomerId(), account.getName(),
					account.getNumber(), account.getPrimaryUserId(), refreshToken);
		}

		System.out.println(refreshToken);
	}

}
