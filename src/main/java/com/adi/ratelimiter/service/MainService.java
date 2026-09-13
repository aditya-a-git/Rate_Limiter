package com.adi.ratelimiter.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MainService {
     public Map<String, List<Long>> rates = new HashMap<>();
}
