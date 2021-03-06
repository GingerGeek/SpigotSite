package be.maximvdw.spigotsite.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSite;
import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.PremiumResource;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.resource.ResourceCategory;
import be.maximvdw.spigotsite.api.resource.ResourceManager;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.user.SpigotUser;
import be.maximvdw.spigotsite.utils.StringUtils;

public class SpigotResourceManager implements ResourceManager {
	private List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();

	public Resource getResourceById(int resourceid) {
		return getResourceById(resourceid, null);
	}

	public Resource getResourceById(int resourceid, User user) {
		try {
			String url = "http://www.spigotmc.org/resources/" + resourceid;
			Map<String, String> params = new HashMap<String, String>();
			Connection.Response res = Jsoup
					.connect(url)
					.method(Method.GET)
					.data(params)
					.cookies(
							user == null ? SpigotSiteCore.getBaseCookies()
									: ((SpigotUser) user).getCookies())
					.userAgent(
							"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
					.execute();
			Document doc = res.parse();
			Element categoryLink = doc.select("a.crumb").last();
			SpigotResource resource = new SpigotResource();

			if (categoryLink.text().toLowerCase().contains("premium"))
				resource = new SpigotPremiumResource();

			String resourceName = doc.title().replace(
					" | SpigotMC - High Performance Minecraft", "");
			resource.setResourceName(resourceName);
			resource.setResourceId(resourceid);

			Element resourceInfo = doc.select("div.resourceInfo").get(0);
			resource.setLastVersion(resourceInfo.select("span.muted").get(0)
					.text());

			Element downloadLink = doc.select("label.downloadButton").get(0)
					.select("a.inner").get(0);
			resource.setDownloadURL("http://www.spigotmc.org/"
					+ downloadLink.attr("href"));

			Element author = doc.select("dl.author").first();
			SpigotUser authorUser = new SpigotUser();
			authorUser.setUsername(author.select("a").first().text());
			authorUser.setUserId(Integer.parseInt(StringUtils.getStringBetween(
					author.select("a").first().attr("href"), "\\.(.*?)/")));
			resource.setAuthor(authorUser);
			return resource;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public List<Resource> getResourcesByUser(User user) {
		List<Resource> resources = getResourcesByUser(user.getUserId());
		for (Resource resource : resources) {
			((SpigotResource) resource).setAuthor(user);
		}
		return resources;
	}

	public List<Resource> getResourcesByUser(int userid) {
		List<Resource> createdResources = new ArrayList<Resource>();
		try {
			String url = "http://www.spigotmc.org/resources/authors/" + userid;
			Map<String, String> params = new HashMap<String, String>();

			Connection.Response res = Jsoup
					.connect(url)
					.method(Method.GET)
					.cookies(SpigotSiteCore.getBaseCookies())
					.data(params)
					.userAgent(
							"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
					.execute();
			Document doc = res.parse();
			String username = StringUtils
					.getStringBetween(doc.title(),
							"Resources from (.*?) | SpigotMC - High Performance Minecraft");
			SpigotUser user = new SpigotUser(username);
			user.setUserId(userid);

			Elements resourceBlocks = doc.select("li.resourceListItem");
			for (Element resourceBlock : resourceBlocks) {
				int id = Integer.parseInt(resourceBlock.id().replace(
						"resource-", ""));
				Element resourceLink = resourceBlock.select("h3.title").get(0)
						.getElementsByTag("a").get(0);

				Element categoryLink = resourceBlock
						.select("div.resourceDetails").select("a").last();
				SpigotResource resource = new SpigotResource();
				if (categoryLink.text().toLowerCase().contains("premium"))
					resource = new SpigotPremiumResource();

				resource.setResourceName(resourceLink.text());
				resource.setAuthor(user);
				resource.setResourceId(id);
				createdResources.add(resource);
			}
		} catch (HttpStatusException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return createdResources;
	}

	public List<Resource> getPurchasedResources(User user) {
		List<Resource> boughtResources = new ArrayList<Resource>();
		try {
			String url = "http://www.spigotmc.org/resources/purchased";
			Map<String, String> params = new HashMap<String, String>();

			Connection.Response res = Jsoup
					.connect(url)
					.method(Method.GET)
					.data(params)
					.cookies(((SpigotUser) user).getCookies())
					.userAgent(
							"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
					.execute();
			Document doc = res.parse();
			Elements resourceBlocks = doc.select("li.resourceListItem");
			for (Element resourceBlock : resourceBlocks) {
				int id = Integer.parseInt(resourceBlock.id().replace(
						"resource-", ""));
				Element resourceLink = resourceBlock.select("h3.title").get(0)
						.getElementsByTag("a").get(0);
				SpigotResource resource = new SpigotResource(
						resourceLink.text());
				resource.setResourceId(id);
				boughtResources.add(resource);
			}
		} catch (HttpStatusException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return boughtResources;
	}

	public List<ResourceCategory> getResourceCategories() {
		List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
		if (this.resourceCategories.size() > 0)
			return this.resourceCategories;
		try {
			String url = "http://www.spigotmc.org/resources/";
			Map<String, String> params = new HashMap<String, String>();

			Connection.Response res = Jsoup
					.connect(url)
					.method(Method.GET)
					.cookies(SpigotSiteCore.getBaseCookies())
					.data(params)
					.userAgent(
							"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
					.execute();
			Document doc = res.parse();
			Element categoryList = doc.select("div.categoryList").first();
			Elements categories = categoryList.select("li");
			for (Element category : categories) {
				Element link = category.select("a").first();
				Element count = category.select("span.count").first();
				SpigotResourceCategory resourceCategory = new SpigotResourceCategory();
				resourceCategory.setCategoryName(link.text());
				String resourceCount = count.text().toString().replace(",", "");
				resourceCategory.setResourceCount(Integer
						.parseInt(resourceCount));
				resourceCategory.setCategoryId(Integer.parseInt(StringUtils
						.getStringBetween(link.attr("href"), "\\.(.*?)/")));
				resourceCategories.add(resourceCategory);
			}

			this.resourceCategories = resourceCategories;
		} catch (HttpStatusException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resourceCategories;
	}

	public List<Resource> getResourcesByCategory(ResourceCategory category) {
		List<Resource> resources = new ArrayList<Resource>();
		try {
			int lastPage = category.getResourceCount() / 20;
			if (category.getResourceCount() % 20 != 0)
				lastPage++;
			for (int i = lastPage; i >= 1; i--) {
				String url = "http://www.spigotmc.org/resources/categories/"
						+ category.getCategoryId() + "/?page=" + i;
				Map<String, String> params = new HashMap<String, String>();

				Connection.Response res = Jsoup
						.connect(url)
						.method(Method.GET)
						.cookies(SpigotSiteCore.getBaseCookies())
						.data(params)
						.userAgent(
								"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
						.execute();
				Document doc = res.parse();
				Elements resourceBlocks = doc.select("li.resourceListItem");
				for (Element resourceBlock : resourceBlocks) {
					int id = Integer.parseInt(resourceBlock.id().replace(
							"resource-", ""));
					Element resourceLink = resourceBlock.select("h3.title")
							.get(0).getElementsByTag("a").get(0);
					SpigotResource resource = new SpigotResource(
							resourceLink.text());
					resource.setResourceId(id);
					Element username = resourceBlock.select("a.username")
							.first();
					Element version = resourceBlock.select("span.version")
							.first();
					resource.setLastVersion(version.text());
					SpigotUser user = new SpigotUser();
					user.setUsername(username.text());
					user.setUserId(Integer.parseInt(StringUtils
							.getStringBetween(username.attr("href"),
									"\\.(.*?)/")));
					resource.setAuthor(user);

					resources.add(resource);
				}
			}
		} catch (HttpStatusException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resources;
	}

	public ResourceCategory getResourceCategoryById(int id) {
		if (this.resourceCategories.size() == 0)
			getResourceCategories();
		for (ResourceCategory category : resourceCategories) {
			if (category.getCategoryId() == id)
				return category;
		}
		return null;
	}

	public String getLastVersion(int resourceid) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<User> getPremiumResourceBuyers(PremiumResource resource,
			User user) throws ConnectionFailedException {
		List<User> buyers = new ArrayList<User>();

		SpigotPremiumResource spigotResource = (SpigotPremiumResource) resource;
		try {
			String url = "http://www.spigotmc.org/resources/"
					+ resource.getResourceId() + "/buyers";
			Map<String, String> params = new HashMap<String, String>();

			Connection.Response res = Jsoup
					.connect(url)
					.method(Method.GET)
					.data(params)
					.cookies(((SpigotUser) user).getCookies())
					.userAgent(
							"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
					.execute();
			Document doc = res.parse();
			Elements buyersBlocks = doc.select("div.member");
			for (Element buyersBlock : buyersBlocks) {
				SpigotUser buyer = new SpigotUser();

				Element userElement = buyersBlock.select("a.username").get(0);
				buyer.setUsername(userElement.text());
				buyer.setUserId(Integer.parseInt(StringUtils.getStringBetween(
						userElement.attr("href"), "\\.(.*?)/")));
				buyers.add(buyer);
			}
		} catch (HttpStatusException ex) {
			throw new ConnectionFailedException();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		spigotResource.setBuyers(buyers);
		return buyers;
	}

	public void addBuyer(PremiumResource resource, User user, User buyer) {
		addBuyer(resource, user, buyer.getUsername());
	}

	public void addBuyer(PremiumResource resource, User user, int userid) {
		User buyer = SpigotSite.getAPI().getUserManager().getUserById(userid);
		addBuyer(resource, user, buyer);
	}

	public void addBuyer(PremiumResource resource, User user, String username) {
		addBuyers(resource, user, new String[] { username });
	}

	public void addBuyers(PremiumResource resource, User user, List<User> buyers) {
		String[] usernames = new String[buyers.size()];
		for (int i = 0; i < buyers.size(); i++)
			usernames[i] = buyers.get(i).getUsername();
		addBuyers(resource, user, usernames);
	}

	public void addBuyers(PremiumResource resource, User user,
			String[] usernames) {
		try {
			String url = "http://www.spigotmc.org/resources/"
					+ resource.getResourceId() + "/add-buyer";
			Map<String, String> params = new HashMap<String, String>();
			String usernamesStr = "";
			for (int i = 0; i < usernames.length; i++)
				usernamesStr += usernames[i] + ",";
			params.put("usernames", usernamesStr);
			params.put("_xfRequestUri",
					"%2Fresources%2Factionbar.1458%2Fadd-buyer");
			params.put("_xfToken",
					((SpigotUser) user).getCookies().get("xf_user"));
			params.put("_xfResponseType", "json");
			params.put("_xfNoRedirect", "1");
			params.put("save", "Save+Changes");
			params.put("_xfConfirm", "1");
			params.put("redirect", "/");

			Connection.Response res = Jsoup
					.connect(url)
					.method(Method.POST)
					.cookies(((SpigotUser) user).getCookies())
					.data(params)
					.userAgent(
							"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0")
					.execute();
			Document doc = res.parse();

		} catch (HttpStatusException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
