configLib = {
    implements='org.keplerproject.luajava.test.node.IConfig',
    processConfigFile = function (filename)
        file = io.open(filename)
        ret = file:read("*a")
        file:close()
        return loadstring(ret)()
    end,
    getName = function (obj)
        return obj.name
    end,
    getChild = function (obj, child)
        for key, t in ipairs(configLib.getChildren(obj)) do
            if configLib.getName(t) == child then return t end
        end
        return nil
    end,
    getChildren = function(t)
        return t.body
    end,
    getAttribute = function(t, attr)
        for k, v in ipairs(t.attributes) do
            if v.name == attr then return v.value end
        end
        return nil
    end,
    getValue = function(t)
        return t.body.value
    end
}