local count = redis.call("INCR", KEYS[1])

if count == 1 then
    redis.call("EXPIRE", KEYS[1], 60)
end

local remaining = redis.call("TTL", KEYS[1])
return { count, remaining }
