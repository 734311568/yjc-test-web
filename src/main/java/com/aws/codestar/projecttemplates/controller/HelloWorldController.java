package com.aws.codestar.projecttemplates.controller;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.aws.codestar.projecttemplates.configuration.ApiService;

import com.aws.codestar.projecttemplates.model.Account;
import com.aws.codestar.projecttemplates.model.Emotion;
import com.aws.codestar.projecttemplates.model.Story;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Basic Spring MVC controller that handles all GET requests.
 */
@RestController
@RequestMapping("/")
public class HelloWorldController {

	@Autowired
	ApiService apiService;

	private final String siteName;

	public HelloWorldController(final String siteName) {
		this.siteName = siteName;
	}

	/**
	 * 访问个人主页
	 *
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/Personalpage")
	public ModelAndView getPersonalpage(@RequestParam(value = "id", required = true, defaultValue = "3") Integer id) throws Exception {

		ModelAndView model = new ModelAndView("Personalpage");
		model.addObject("xmlSource", apiService.getHomepage(id));
		System.out.println("id\t" + id);
		return model;

	}

	/**
	 * 传前台图片的路径字符串
	 * @param id
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "/img")
	public String getString(@RequestParam(value = "id", required = true) Integer id) throws IOException {
	        System.out.println("id\t"+id);
		return  apiService.getImg(id);
	}
	/**
	 * 導向登入後頁面
	 *
	 * @param response
	 * @param httpSession
	 * @param code 可為空, 以Line API 登入時傳入Line.code
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws URISyntaxException
	 */
	@RequestMapping(value = "/")
	public ModelAndView index(HttpServletResponse response, HttpSession httpSession,
		@RequestParam(required = false) String code)
		throws IOException, ParserConfigurationException, TransformerException, URISyntaxException {
		JSONObject jSONObject = null;

		// 取得 Line Token, 並解析資料以得 email 資訊
		if (code != null) {
			// thirdParty = "line";
			jSONObject = getToken(code);

			if (jSONObject != null) {
				String profile = jSONObject.getString("profile");
				System.out.println("get email from Line: " + jSONObject.getString("email"));
				String userName = find(httpSession, "line", new JSONObject(profile).getString("userId"), "");
				System.out.println("get userName from Line: " + userName);
			} else {
				return null;
			}
		}

		if (httpSession.getAttribute("requestURI") != null) {
			// response.sendRedirect(httpSession.getAttribute("requestURI").toString());
		}
		ModelAndView model = new ModelAndView("index");
		model.addObject("xmlSource", apiService.getStory(httpSession));
		return model;
	}

	/**
	 * 呼叫 apiService.findOneBythirdParty(),判斷登入帳號是否存在,存在就設定session.attribute
	 *
	 * @param httpSession
	 * @param thirdParty 第三方登入API
	 * @param userId 第三方供應商提供之使用者ID
	 * @param userEmail 可為空, 有值表示為BBMall會員
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void getId(HttpSession httpSession, String thirdParty, String userId, String userEmail)
		throws IOException, URISyntaxException {
		JSONObject jSONObject = null;

		if (userEmail != null && userEmail.length() > 0) {
			jSONObject = apiService.findOneByEmail(userEmail);
		} else {
			jSONObject = apiService.findOneByThirdPartyId(thirdParty, userId);
		}

		if (jSONObject != null) {
			String uuid = jSONObject.get("universallyUniqueIdentifier").toString();
			String personnelId = jSONObject.get("id").toString();
			String nickname = jSONObject.get("nickname").toString();
			String email = jSONObject.get("email").toString();
			String personnelHref = jSONObject.getJSONObject("_links").getJSONObject("self").get("href").toString();

			httpSession.setAttribute("me", uuid);
			httpSession.setAttribute("id", personnelId);
			httpSession.setAttribute("personnelHref", personnelHref);
			httpSession.setAttribute("thirdParty", thirdParty);
			httpSession.setAttribute("nickname", nickname);
			httpSession.setAttribute("email", email);
		}
	}

	/**
	 * 註冊頁面
	 *
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@RequestMapping(value = "/registerview")
	public ModelAndView registerview(HttpServletRequest request) throws IOException, ParserConfigurationException {

		// builds absolute path of the XML file
		String xmlFile = "resources/xml.xml";
		String contextPath = request.getServletContext().getRealPath("");
		String xmlFilePath = contextPath + File.separator + xmlFile;
		Source source = new StreamSource(new File(xmlFilePath));

		ModelAndView model = new ModelAndView("register");
		model.addObject("xmlSource", source);
		return model;
	}

	/**
	 * 註冊動作, call apiService.registerUser() 新增帳號資料
	 *
	 * @param account
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@RequestMapping(value = "/register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE) // ,
	// produces
	// =
	// MediaType.APPLICATION_JSON_UTF8_VALUE
	public String register(Account account) throws IOException, ParserConfigurationException {
		// System.out.println("firstName:" + account.getFirstName());
		// System.out.println("lastName:" + account.getLastName());
		// System.out.println("email:" + account.getEmail());
		// System.out.println("password:" + account.getPassword());
		// System.out.println("confirmpassword:" + account.getConfirmPassword());
		return apiService.registerUser(account.getFirstName(), "", "", "", account.getEmail(), account.getLastName(),
			account.getFirstName(), "", "", "");
	}

	/**
	 * 帳號登入頁面
	 *
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@RequestMapping(value = "/login")
	public ModelAndView login(HttpServletRequest request) throws IOException, ParserConfigurationException {
		String xmlFile = "resources/xml.xml";
		String contextPath = request.getServletContext().getRealPath("");
		String xmlFilePath = contextPath + File.separator + xmlFile;
		Source source = new StreamSource(new File(xmlFilePath));

		ModelAndView model = new ModelAndView("login");
		model.addObject("xmlSource", source);
		return model;
	}

	/**
	 * 登出動作, 移除session.attrbute並回到首頁
	 *
	 * @param response
	 * @param httpSession
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws URISyntaxException
	 */
	@RequestMapping(value = "/logout")
	public ModelAndView logout(HttpServletResponse response, HttpSession httpSession)
		throws IOException, ParserConfigurationException, TransformerException, URISyntaxException {
		httpSession.removeAttribute("me");
		httpSession.removeAttribute("thirdParty");
		httpSession.removeAttribute("nickname");
		httpSession.removeAttribute("email");
		return index(response, httpSession, null);
	}

	@RequestMapping(value = "/memberCenter", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ModelAndView memberCenter(HttpServletRequest request, HttpServletResponse response, HttpSession httpSession)
		throws IOException, ParserConfigurationException, TransformerException, URISyntaxException {
		if (httpSession.getAttribute("me") == null) {
			System.out.println("getRequestURI: " + request.getRequestURI());
			httpSession.setAttribute("requestURI", request.getRequestURI());
			return login(request);
		}

		String xmlFile = "resources/xml.xml";
		String contextPath = request.getServletContext().getRealPath("");
		String xmlFilePath = contextPath + File.separator + xmlFile;
		Source source = new StreamSource(new File(xmlFilePath));

		ModelAndView model = new ModelAndView("memberCenter");
		model.addObject("xmlSource", source);

		return model;
	}

	/**
	 * 導向 Line 登入驗證
	 *
	 * @return Line 登入驗證 URL
	 */
	@RequestMapping(value = "/line")
	public String lineHref() {
		return "https://access.line.me/oauth2/v2.1/authorize?response_type=code" + "&client_id="
			+ System.getenv("LINE_CLIENT_ID") + "&redirect_uri=" + System.getenv("LINE_REDIRECT_URI") + "&state="
			+ getRandomString() + "&scope=" + "openid%20profile%20email" + "&nonce=" + getRandomString();
	}

	/**
	 * 產生隨機字串
	 *
	 * @return 隨機字串
	 */
	public String getRandomString() {
		SecureRandom RANDOM = new SecureRandom();
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);

		return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * 搜尋帳號是否存在(Facebook, Google, Line)
	 *
	 * @param httpSession
	 * @param thirdParty 第三方登入API
	 * @param userId 第三方供應商提供之使用者ID
	 * @param userEmail 可為空, 有值表示為BBMall會員
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws URISyntaxException
	 */
	@RequestMapping(value = "/find", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String find(HttpSession httpSession, @RequestParam(required = false) String thirdParty,
		@RequestParam(required = false) String userId, @RequestParam(required = false) String userEmail)
		throws IOException, ParserConfigurationException, URISyntaxException {
		JSONObject jSONObject = null;
		getId(httpSession, thirdParty, userId, userEmail);

		if (userEmail != null && userEmail.length() > 0) {
			jSONObject = apiService.findOneByEmail(userEmail);
		} else {
			jSONObject = apiService.findOneByThirdPartyId(thirdParty, userId);
		}
		if (jSONObject == null) {
			return "沒有這個帳號哦！";
		}

		return jSONObject.getString("nickname");
	}

	/**
	 * 留言
	 *
	 * @param story
	 * @param httpSession
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@RequestMapping(value = "/postComment", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String postComment(Story story, HttpSession httpSession)
		throws IOException, ParserConfigurationException, TransformerException {
		if (httpSession.getAttribute("me") == null) {
			return null;
		}

		System.out.println("storyId:" + story.getStoryId());
		System.out.println("storyHref:" + story.getStoryHref());
		System.out.println("who:" + story.getWho());
		System.out.println("content:" + story.getContent());
		// story:who=me&story=1687f4ee-d961-4042-bee7-5baeffe6b90c&content=
		System.out.println(apiService.postComments(story.getContent(), story.getWho(), story.getStoryHref()));

		JSONArray jSONArrayComment = apiService.getStoryComment(story.getStoryId());
		String[] stringsOfImgurls = new String[jSONArrayComment.length()];
		JSONArray jSONArray = new JSONArray();

		for (int i = 0; i < jSONArrayComment.length(); i++) {
			JSONObject jSONObject = new JSONObject();
			jSONObject.put("who", jSONArrayComment.getJSONObject(i).get("who").toString());
			jSONObject.put("content", jSONArrayComment.getJSONObject(i).get("content").toString());
			jSONArray.put(jSONObject);
//			stringsOfImgurls[i] = jSONObject;
			//[{userName: 'pc', content: '這是一則留言'}, {userName: 'chiah', content: '這是第二則留言'}];
		}

		System.out.println("jSONArray: " + jSONArray);
		return jSONArray.toString();
	}

	/**
	 * 按讚
	 *
	 * @param emotion
	 * @param httpSession
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@RequestMapping(value = "/like", method = RequestMethod.POST)
	public String like(Emotion emotion, HttpSession httpSession) throws IOException, ParserConfigurationException {
		if (httpSession.getAttribute("me") == null) {
			return null;
		}
		System.out.println("story:" + emotion.getStory());
		System.out.println("who:" + emotion.getWho());
		return emotion.toString();
	}

	/**
	 * 收藏
	 *
	 * @param emotion
	 * @param httpSession
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@RequestMapping(value = "/bookmark", method = RequestMethod.POST)
	public String bookmark(Emotion emotion, HttpSession httpSession) throws IOException, ParserConfigurationException {
		if (httpSession.getAttribute("me") == null) {
			return null;
		}
		System.out.println("story:" + emotion.getStory());
		System.out.println("who:" + emotion.getWho());
		return emotion.toString();
	}

	/**
	 * 取得 Line Token
	 *
	 * @param code
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public JSONObject getToken(String code) throws IOException, ParserConfigurationException {
		JSONObject jSONObject = new JSONObject();

		// 建立 CloseableHttpClient & HttpPost
		CloseableHttpClient closeableHttpClient = HttpClients.createDefault();

		// 取得 access token
		HttpPost httpPost = new HttpPost("https://api.line.me/oauth2/v2.1/token");

		// 設定 Request parameters and other properties.
		// 取得 access_token 所需參數如下:
		// 參數值為 https://developers.line.biz/console/channel/... 設定之內容
		// 108.03.04 以下設定為系統參數(tomcat setenv.bat)
		List<NameValuePair> params = new ArrayList<>(2);
		params.add(new BasicNameValuePair("grant_type", System.getenv("LINE_GRANT_TYPE")));
		params.add(new BasicNameValuePair("code", code));
		params.add(new BasicNameValuePair("redirect_uri", System.getenv("LINE_REDIRECT_URI")));
		params.add(new BasicNameValuePair("client_id", System.getenv("LINE_CLIENT_ID")));
		params.add(new BasicNameValuePair("client_secret", System.getenv("LINE_CLIENT_SECRET")));
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

		// Execute and get the response.
		CloseableHttpResponse closeableHttpResponseOfToken = closeableHttpClient.execute(httpPost);
		HttpEntity httpEntityOfToken = closeableHttpResponseOfToken.getEntity();

		String stringToken = "";

		System.out.println("token--getStatusLine: " + closeableHttpResponseOfToken.getStatusLine());
		if (closeableHttpResponseOfToken.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			if (httpEntityOfToken != null) {
				stringToken = EntityUtils.toString(httpEntityOfToken, "UTF-8");
				System.out.println("token: " + stringToken);
			} else {
				jSONObject.put("errorMessage", "ERROR! closeableHttpResponseOfToken.getEntity() = null");
				return jSONObject;
			}
		} else {
			System.out.println("token--ERROR");
			jSONObject.put("errorMessage", "ERROR! closeableHttpResponseOfToken.getStatusLine() = "
				+ closeableHttpResponseOfToken.getStatusLine());
			return jSONObject;
		}

		System.out.println("token--httppost: " + httpPost.toString());

		JSONObject jsonObjectOfToken = new JSONObject(stringToken);
		System.out.println("token--finished");
		closeableHttpResponseOfToken.close();

		String[] jwtArray = decodeJWT(jsonObjectOfToken.get("id_token").toString(), "Line");
		if (jwtArray == null) {
			System.out.println("decodeJWT Error!--getToken");
			jSONObject.put("errorMessage", "ERROR! decode JWT failed");
			return jSONObject;
		} else {
			JSONObject jsonObjectJWT = new JSONObject(jwtArray[1]);
			System.out.println("jsonObjectJWT: " + jsonObjectJWT.toString());
			System.out.println("JWTemail: " + jsonObjectJWT.getString("email"));
			System.out.println(Arrays.toString(jwtArray));
			jSONObject.put("email", jsonObjectJWT.getString("email"));
		}

		// 取得 user profiles
		HttpGet httpGet = new HttpGet("https://api.line.me/v2/profile");

		// 設定 Authorization
		httpGet.setHeader("Authorization", "Bearer " + jsonObjectOfToken.get("access_token").toString());

		CloseableHttpResponse closehttpResponseOfProfile = closeableHttpClient.execute(httpGet);
		HttpEntity httpEntityOfProfile = closehttpResponseOfProfile.getEntity();

		String stringProfile = "";

		System.out.println("profile--getStatusLine: " + closehttpResponseOfProfile.getStatusLine());
		if (closehttpResponseOfProfile.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			if (httpEntityOfProfile != null) {
				stringProfile = EntityUtils.toString(httpEntityOfProfile, "UTF-8");

				JSONObject jsonObjectProfile = new JSONObject(stringProfile);

				System.out.println("profile: " + stringProfile);
				System.out.println("--------------------------");
				System.out.println("userId: " + jsonObjectProfile.getString("userId"));
				System.out.println("displayName: " + jsonObjectProfile.getString("displayName"));
				System.out.println("pictureUrl: " + jsonObjectProfile.getString("pictureUrl"));
				System.out.println("statusMessage: " + jsonObjectProfile.getString("statusMessage"));
				System.out.println("--------------------------");

				jSONObject.put("profile", stringProfile);
			} else {
				jSONObject.put("errorMessage", "ERROR! closehttpResponseOfProfile.getEntity() = null");
				return jSONObject;
			}
		} else {
			System.out.println("profile--ERROR");
			jSONObject.put("errorMessage", "ERROR! closehttpResponseOfProfile.getStatusLine() = "
				+ closehttpResponseOfProfile.getStatusLine());
			return jSONObject;
		}
		System.out.println("profile--httppost: " + httpGet.toString());
		return jSONObject;
	}

	/**
	 * Json Web Token 轉譯
	 *
	 * @param idtoken 要轉譯的 JWT
	 * @param thirdParty公司
	 * @return 傳回一陣列, JWT經轉譯後的內容, 格式為{header, body, signature}
	 */
	private String[] decodeJWT(String idtoken, String thirdParty) throws UnsupportedEncodingException {

		String jwtToken = idtoken;
		if (jwtToken == null) {
			return null;
		}
		System.out.println("------------- idtoken --------------");
		System.out.println("(" + thirdParty + ")" + jwtToken);
		System.out.println("------------------------------------");

		System.out.println("------------ Decode JWT ------------");

		String[] jwtTokenArray = jwtToken.split("\\.");
		String base64EncodedHeader = jwtTokenArray[0];
		String base64EncodedBody = jwtTokenArray[1];
		String base64EncodedSignature = jwtTokenArray[2];

		Base64 base64Url = new Base64(true);
		String header = new String(base64Url.decode(base64EncodedHeader), "UTF-8");
		System.out.println("(" + thirdParty + ") JWT Header : " + header);

		String body = new String(base64Url.decode(base64EncodedBody), "UTF-8");
		System.out.println("(" + thirdParty + ") JWT Body : " + body);
		System.out.println("(" + thirdParty + ") JWT Body.sub : " + new JSONObject(body).get("sub").toString());

		String signature = new String(base64Url.decode(base64EncodedSignature), "UTF-8");
		System.out.println("(" + thirdParty + ") JWT Signature : " + signature);

		System.out.println("---------------- 完成 ---------------");
		String[] result = {header, body, signature};

		// final Base64 base64 = new Base64();
		// final String text = "字串文字";
		// final byte[] textByte = text.getBytes("UTF-8");
		// //編碼
		// final String encodedText = base64.encodeToString(textByte);
		// System.out.println("字串文字-編碼: " + encodedText);
		// //解碼
		// System.out.println("字串文字-解碼: " + new String(base64.decode(encodedText),
		// "UTF-8"));
		return result;
	}

	/**
	 * 發文
	 *
	 * @param imgUrls
	 * @param who
	 * @param storyContent
	 * @param storyHref
	 * @param request
	 * @param httpSession
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/postStory", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String postStory(
		@RequestParam(required = false) String imgUrls,
		@RequestParam(required = false) String who,
		@RequestParam(required = false) String storyContent,
		HttpSession httpSession) throws Exception {
		if (httpSession.getAttribute("me") == null) {
			return null;
		}
		JSONObject jSONObject = new JSONObject(imgUrls);

		System.out.println("jSONObject = " + jSONObject.toString());

		JSONArray jSONArray = jSONObject.getJSONArray("urls");
		String[] stringsOfImgUrls = new String[jSONArray.length()];

		for (int i = 0; i < jSONArray.length(); i++) {
			stringsOfImgUrls[i] = jSONArray.getJSONObject(i).get("url").toString();
		}
		apiService.postStory(storyContent, who, stringsOfImgUrls);

		return "發文成功";
	}

	@RequestMapping(value = "/postImgUrl", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String postImgUrl(@RequestParam(required = false) String url, @RequestParam(required = false) String content, String storyUrl, HttpSession httpSession) throws IOException {
		if (httpSession.getAttribute("me") == null) {
			return null;
		}
		System.out.println("url: " + url);
		System.out.println("content: " + content);
		System.out.println(apiService.postImgUrl(url, content, ""));
		return "圖片上傳成功";
	}
}
