print(a)

function run1()
    a:a('string')
    a:a(1)
    a:a(2)
    a:a(true)
    a:a('c')
    a:a({"abc", nil, "def"})
    a:a({nil, 2, nil, 3})
    a:a({1, 2, 3, nil})
    a:a({1, 2, nil, 3})
    a:a({1, 2, 3, 4})
    a:b(1, 2, 3, 4, 5, "1")
    a:b(2, 2, 2, 2, 2, 2)
    a:b(1, 2, 3, 4, 5, 6, 7, 8, 9, true)
    a:b(2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
end

function run2()
    run1()
end

function run3()
    run2()
end

run3()