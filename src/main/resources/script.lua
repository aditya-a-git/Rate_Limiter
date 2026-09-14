local count = redis.call("INCR", KEYS[1])

if tonumber(count) == 1 then
    redis.call("EXPIRE", KEYS[1], 60)
end

return count
