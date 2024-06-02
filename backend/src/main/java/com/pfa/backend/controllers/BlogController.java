package com.pfa.backend.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfa.backend.entities.Blog;
import com.pfa.backend.entities.Pathologie;
import com.pfa.backend.entities.User;
import com.pfa.backend.repositories.BlogRepository;
import com.pfa.backend.repositories.PathologieRepository;
import com.pfa.backend.repositories.UserRepository;
import com.pfa.backend.responseClass.LlamaResponse;
import com.pfa.backend.responseClass.pythonApiResponse;


@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class BlogController {

    @Value("${serverpython.port}")
    private Integer port;

    @Value("${spring.ai.ollama.base-url}")
    private String urlLlama;

    @Autowired
    private BlogRepository blogRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PathologieRepository pathologieRepo;


    @PostMapping("/blogs")
    public ResponseEntity<Blog> createBlog(@RequestBody Blog blog){
        Blog savedBlog = blogRepo.save(blog);
        User user = blog.getUser();
        User user1 = userRepo.findByEmail(user.getEmail()).orElse(null);
        if(user1==null){
            userRepo.save(user);
        }
        return new ResponseEntity<>(savedBlog,HttpStatus.CREATED);
    }

    @PutMapping("Blog/{id}")
    public ResponseEntity<Blog> modifyBlog(@PathVariable("id") String id, @RequestBody Blog Blog) {
        Blog Blogfound = blogRepo.findById(id).orElse(null);
        if(Blogfound!=null){
            Blog.setIdBlog(id);
            blogRepo.save(Blog);
            return new ResponseEntity<>(Blog,HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("Blog/{id}")
    public ResponseEntity<Blog> modifyPartiallyBlog(@PathVariable("id") String id, @RequestBody Blog Blog) {
        Blog Blogfound = blogRepo.findById(id).orElse(null);
        if(Blogfound!=null){
            Optional.ofNullable(Blog.getPathologie()).ifPresent(Blogfound::setPathologie);
            Optional.ofNullable(Blog.getPostTime()).ifPresent(Blogfound::setPostTime);
            Optional.ofNullable(Blog.getTags()).ifPresent(Blogfound::setTags);
            Optional.ofNullable(Blog.getUser()).ifPresent(Blogfound::setUser);
            Optional.ofNullable(Blog.getUpvotes()).ifPresent(Blogfound::setUpvotes);
            Optional.ofNullable(Blog.getDownvotes()).ifPresent(Blogfound::setDownvotes);
            
            blogRepo.save(Blogfound);
            return new ResponseEntity<>(Blogfound,HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/blog/{id}")
    public ResponseEntity<Blog> getBlog(@PathVariable("id") String id) {
        Blog blog = blogRepo.findById(id).orElse(null);
        return new ResponseEntity<>(blog,HttpStatus.FOUND);
    }

    @GetMapping("/blogs")
    public ResponseEntity<List<Blog>> getBlogs() {
        List<Blog> blogs = blogRepo.findAll();
        return new ResponseEntity<>(blogs,HttpStatus.OK);
    }

    @DeleteMapping("/blog/{id}")
    public ResponseEntity<String> deleteBlog(@PathVariable("id") String id) {
        blogRepo.deleteById(id);
        return new ResponseEntity<String>("blog deleted",HttpStatus.NO_CONTENT);
    }

    @PostMapping("/blog/{id}")
    public ResponseEntity<String> reactBlog(@PathVariable("id") String id,@RequestParam Long choice) {
        Blog blog = blogRepo.findById(id).orElse(null);
        if(choice>0) blog.upvotes++;
        else if(choice<0) blog.downvotes++;
        blogRepo.save(blog);
        return new ResponseEntity<>("reaction saved",HttpStatus.OK);
    }

    @GetMapping("/fetchListBlog/{topic}")
    public ResponseEntity<pythonApiResponse> fetchListBlog(@PathVariable("topic") String topic) {
        String url = "http://localhost:"+port+"/listThreads/"+topic;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<pythonApiResponse> response = restTemplate.getForEntity(url, pythonApiResponse.class);
        return new ResponseEntity<>(response.getBody(),HttpStatus.FOUND);
    }

    @GetMapping("/fetchedBlog/{choice}")
    public ResponseEntity<Pathologie> fetchSelectedBlog(@PathVariable("choice") String choice) {
        String url = "http://localhost:"+port+"/selectedThread/"+choice;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<pythonApiResponse> response = restTemplate.getForEntity(url, pythonApiResponse.class);
        Pathologie pathologie = mapText(response.getBody().getText());
        pathologieRepo.save(pathologie);
        Blog blog = Blog.builder()
                        .downvotes(0)
                        .pathologie(pathologie)
                        .postTime(new Date())
                        .tags(new ArrayList<>())
                        .upvotes(0)
                        .user(null)
                        .build();
        blogRepo.save(blog);
        return new ResponseEntity<>(pathologie,HttpStatus.FOUND);
    }

    public Pathologie mapText(String text) {
        String url = urlLlama+"api/v1/ai/generate?promptMessage="+text;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<LlamaResponse> response = restTemplate.getForEntity(url, LlamaResponse.class);
        String jsonresponse = response.getBody().getMessage();
        ObjectMapper objectMapper = new ObjectMapper();
        Pathologie pathologie = new Pathologie();
        try {
            pathologie = objectMapper.readValue(jsonresponse, Pathologie.class);
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pathologie;
    }
}
