package uk.ac.man.cs.eventlite.mastodon;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.method.Statuses;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Timelines;

import okhttp3.OkHttpClient;

@Service
public class MastodonService {
	String accessToken = "Mastodon-API";
    MastodonClient client = new MastodonClient.Builder("mastodonapp.uk", new OkHttpClient.Builder(), new Gson())
            .accessToken(accessToken).build();
    
    public boolean makePost(String content) {
    	Statuses status = new Statuses(client);
    	try {
    		MastodonRequest<Status> req = status.postStatus(content, null, null, false,
    				null, Status.Visibility.Unlisted);
    		req.execute();
    	}catch(Mastodon4jRequestException e) {
    		return false;
    	}
    	
    	return true;
    }
    
    public List<Post> lastThreePosts() {
    	Timelines timelines = new Timelines(client);
    	List<Post> posts = new ArrayList<Post>();
    	try {
    		Pageable<Status> home = timelines.getHome(new Range()).execute();
    		List<Status> statuses = home.getPart().stream().limit(3).collect(Collectors.toList());
    		statuses.forEach(status -> {
    			Post post = new Post();
    			post.setContent(status.getContent().replaceAll("<[^>]*>", ""));
    			post.setUri(status.getUri());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                try {
                	Date d = sdf.parse(status.getCreatedAt());
                	String formattedDate = dateFormat.format(d);
                	String formattedTime = timeFormat.format(d);
                	LocalDate date = LocalDate.parse(formattedDate);
                	LocalTime time = LocalTime.parse(formattedTime);
                	post.setDate(date);
                	post.setTime(time);
                }catch(Exception e) {}
                posts.add(post);
            });
    	}catch(Mastodon4jRequestException e) {}
    	
    	return posts;
    }
}
