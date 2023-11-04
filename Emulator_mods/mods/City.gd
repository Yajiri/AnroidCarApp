extends Reference

var mod_name: String = "City"

func init(global) -> void:
	global.register_environment("City/City", preload("res://src/environments/City/City.tscn"))
