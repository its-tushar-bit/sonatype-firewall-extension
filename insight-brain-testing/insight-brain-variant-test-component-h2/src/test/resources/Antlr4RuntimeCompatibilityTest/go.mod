module example.com/my/thing

go 1.12

require example.com/other/thing v1.0.2
exclude example.com/old/thing v1.2.3
replace example.com/bad/thing v1.2.3 => example.com/good/thing v1.4.5
